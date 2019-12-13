package com.zhongfei.scheduler.network

import akka.actor.typed.scaladsl.{ActorContext, Behaviors, TimerScheduler}
import akka.actor.typed.{ActorRef, Behavior}
import com.zhongfei.scheduler.network.SchedulerConnection._
import com.zhongfei.scheduler.network.SchedulerConnectionManager.{ActiveServer, Unreachable}
import com.zhongfei.scheduler.transport.Node
import io.netty.channel.ChannelFuture

import scala.collection.immutable.Queue
import scala.concurrent.duration.{Deadline, FiniteDuration}

object SchedulerConnection {

  trait Message

  trait Command extends Message

  trait Event extends Message

  //心跳事件响应
  case class HeartBeaten(actionId: Long, hosts: Option[String]) extends Event

  case object SendHeatBeat extends Command

  case object CheckHeartBeatIntervalTimeout extends Command

  //初始化启动命令
  case object Initialize extends Command

  //初始化超时
  case object InitializeTimeout extends Command

  //初始化结果
  case class Initialized(success: Boolean) extends Event

  //重新连接
  case object Reconnect extends Command

  //不同状态下暂存的数据
  trait Data

  //初始化
  case object Uninitialized extends Data

  //暂存数据
  case class Cache(commands: Queue[Command]) extends Data

  def apply(option: ClientOption, node: Node, manager: ActorRef[SchedulerConnectionManager.Message]): Behavior[Message] =
    Behaviors.setup { context =>
      Behaviors.withTimers { timers =>
        new SchedulerConnection(option, node, timers, manager, context).down()
      }
    }
}

/**
 * 服务连接对象
 *
 * @param option
 * @param node
 * @param timers
 * @param context
 */
class SchedulerConnection(option: ClientOption, node: Node, timers: TimerScheduler[Message], manager: ActorRef[SchedulerConnectionManager.Message], context: ActorContext[Message]) {

  /**
   * 开始为下线状态
   *
   * @return
   */
  def down(): Behavior[Message] = Behaviors.receiveMessage {
    //接收初始化消息,执行初始化
    case Initialize =>
      //创建超时消息
      timers.startSingleTimer(InitializeTimeout, InitializeTimeout, option.iniTimout)
      val self = context.self
      //创建客户端
      val client = new SchedulerClient(node, self)
      client.init().addListener { (future: ChannelFuture) =>
        //返回初始化结果
        self ! Initialized(future.isSuccess)
      }
      up(Deadline.now.time)
    //如果提前接收到了初始化结束请求
    case Initialized(success) => initialized(success)

  }

  /**
   * 初始化结果方法执行
   * @param success
   * @return
   */
  def initialized(success:Boolean): Behavior[Message] ={
    if (success) {
      timers.cancel(InitializeTimeout)
      //成功上线后，定时发送心跳消息
      timers.startTimerWithFixedDelay(SendHeatBeat, SendHeatBeat, option.sendHeartBeatInterval)
      //定时检测是否在线
      timers.startTimerWithFixedDelay(CheckHeartBeatIntervalTimeout, CheckHeartBeatIntervalTimeout, option.checkHeartBeatOnCloseInterval)
      //并通知管理器服务已经上线
      manager
      //转移到上线状态
      up(Deadline.now.time)
    } else {
      //如果没连接上就重试
      timers.startTimerWithFixedDelay(Reconnect, Reconnect, option.reconnectInterval)
      down()
    }
  }
  def up(lastedHeartBeatTime: FiniteDuration): Behavior[Message] = Behaviors.receiveMessage {
    //定时向服务器发送心跳请求
    case SendHeatBeat =>
      sendHeartBeat
      Behaviors.same
    //收到心跳请求后，处理
    case HeartBeaten(actionId, hosts: Option[String]) =>
      //将返回的服务器数据交给连接管理器处理
      manager ! ActiveServer(hosts)
      up(Deadline.now.time)
    //检查心跳超时
    case CheckHeartBeatIntervalTimeout =>
      //当前时间减去最后一次记录的时间如果大于时间间隔就将连接设置为不可达
      val interval = Deadline.now - lastedHeartBeatTime
      if (interval.time > option.checkHeartBeatOnCloseInterval) {
        manager ! Unreachable(node.uri())
        unreachable()
      } else {
        Behaviors.same
      }
      //接收不可达命令
    case Unreachable(_) => unreachable()

  }

  /**
   * 将hosts转换为节点集合
   * @param hosts
   * @return
   */
  implicit def node(hosts: Option[String]): Option[Set[Node]] ={
    // TODO: 需要测试边缘情况，譬如只有一个server时
    hosts match {
      case Some(value) =>{
        val hosts = value.split(",")
        val nodes = hosts.collect(host => {
          val strings = host.split(":")
          Node(strings(0), strings(1).toInt)
        })
        Some(nodes.toSet)
      }
      case None => None
    }
  }



  /**
   * 不可达状态
   * @return
   */
  def unreachable(): Behavior[Message] = Behaviors.receiveMessage {
  Behaviors.same
  }

  def sendHeartBeat(): Unit = {
    //    val transfer = context.spawnAnonymous(NettyTransfer(peer.channel,option.transferRetryCount,option.transferRetryInterval))
    //    transfer ! WrappedRequest(Request(actionId = IDGenerator.next(),actionType = ActionTypeEnum.HeartBeat.id.toByte))
  }
}
