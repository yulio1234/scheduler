package com.zhongfei.scheduler.network

import akka.actor.typed.scaladsl.{ActorContext, Behaviors, TimerScheduler}
import akka.actor.typed.{ActorRef, Behavior}
import com.zhongfei.scheduler.network.Dispatcher.{HeartBeat, HeartBeaten}
import com.zhongfei.scheduler.network.SchedulerConnection._
import com.zhongfei.scheduler.network.SchedulerConnectionManager.{Connected, ServerActive, ServerTerminate, Unreachable}
import com.zhongfei.scheduler.transport.{Node, Peer}
import com.zhongfei.scheduler.utils.IDGenerator
import io.netty.channel.ChannelFuture

import scala.collection.immutable.Queue
import scala.concurrent.duration.{Deadline, FiniteDuration}

object SchedulerConnection {

  trait Message

  trait Command extends Message

  trait Event extends Message


  case object SendHeatBeat extends Command

  case object CheckHeartBeatIntervalTimeout extends Command

  //初始化启动命令
  case object Initialize extends Command

  //初始化超时
  case object InitializeTimeout extends Command

  //初始化结果
  case class Initialized(success: Boolean, peer: Peer) extends Event

  //重新连接
  case object Reconnect extends Command

  //不同状态下暂存的数据
  trait Data

  //初始化
  case object Uninitialized extends Data

  //暂存数据
  case class Cache(commands: Queue[Command]) extends Data

  def apply(option: ClientOption,
            node: Node,
            manager: ActorRef[SchedulerConnectionManager.Message],
            dispatcher: ActorRef[Dispatcher.Message],
            schedulerClient: SchedulerClient): Behavior[Message] =
    Behaviors.setup { context =>
      Behaviors.withTimers { timers =>
        new SchedulerConnection(option, node, timers, manager,dispatcher,schedulerClient, context).down()
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
class SchedulerConnection(option: ClientOption,
                          node: Node,
                          timers: TimerScheduler[Message],
                          manager: ActorRef[SchedulerConnectionManager.Message],
                          dispatcher: ActorRef[Dispatcher.Message],
                          client:SchedulerClient,context: ActorContext[Message]) {

  case class WrappedHeartBeaten(response:Dispatcher.Message) extends Event

  private val heartBeatenAdapter: ActorRef[Dispatcher.Message] = context.messageAdapter(WrappedHeartBeaten)

  /**
   * 开始为下线状态
   *
   * @return
   */
  def down(): Behavior[Message] = Behaviors.receiveMessage {
    //接收初始化消息,执行初始化
    case Initialize =>
      context.log.debug("接收到初始化请求")
      //创建超时消息
      timers.startSingleTimer(InitializeTimeout, InitializeTimeout, option.iniTimout)
      val self = context.self
      //创建客户端
      client.createConnection(node).addListener { (future: ChannelFuture) =>
        //返回初始化结果
        context.log.debug("注册初始化响应监听")
        self ! Initialized(future.isSuccess, Peer(node.host, node.port, future.channel()))
      }
      Behaviors.same
    //如果提前接收到了初始化结束请求
    case Initialized(success, peer) =>
      timers.cancel(InitializeTimeout)
      if (success) {
        //成功上线后，定时发送心跳消息
        timers.startTimerWithFixedDelay(SendHeatBeat, SendHeatBeat, option.sendHeartBeatInterval)
        //定时检测是否在线
        timers.startTimerWithFixedDelay(CheckHeartBeatIntervalTimeout, CheckHeartBeatIntervalTimeout, option.checkHeartBeatOnCloseInterval)
        //初始化成功后立即发送心跳
        dispatcher ! HeartBeat(IDGenerator.next(),option.appName,peer,heartBeatenAdapter)
        //并通知管理器服务已经上线
        manager ! Connected(node.uri(), context.self)
        context.log.debug(s"链接成功，转换为上线状态：$peer")
        timers.cancel(Reconnect)
        //转移到上线状态
        up(Deadline.now.time, peer)
      } else {
        //如果没连接上不可达
        //告诉管理器不可达
        manager ! Unreachable(node.uri())
        //尝试重连
        timers.startSingleTimer(Reconnect, Reconnect, option.reconnectInterval)
        context.log.warn(s"网络链接建立失败，无法与服务端通讯：$peer")
        Behaviors.same
      }
    //如果创建一直没回应，也重连
    case InitializeTimeout =>
      manager ! Unreachable(node.uri())
      context.self ! Initialize
      Behaviors.same
    case Reconnect =>
      context.self ! Initialize
      Behaviors.same
    case ServerTerminate => Behaviors.stopped
  }

  def up(lastedHeartBeatTime: FiniteDuration, peer: Peer): Behavior[Message] = Behaviors.receiveMessage {
    //定时向服务器发送心跳请求
    case SendHeatBeat =>
      context.log.debug(s"服务已在线，发送心跳请求：$peer")
      dispatcher ! HeartBeat(IDGenerator.next(),option.appName,peer,heartBeatenAdapter)
      Behaviors.same
    //收到心跳请求后，处理
    case WrappedHeartBeaten(response) if response.isInstanceOf[HeartBeaten]=>
      //将返回的服务器数据交给连接管理器处理
      up(Deadline.now.time, peer)
    //检查心跳超时
    case CheckHeartBeatIntervalTimeout =>
      //当前时间减去最后一次记录的时间如果大于时间间隔就将连接设置为不可达
      val interval = Deadline.now - lastedHeartBeatTime
      if (interval.time > option.checkHeartBeatOnCloseInterval) {
        //给服务器发送不可达消息
        manager ! Unreachable(node.uri())
        //关闭心跳检查
        timers.cancel(CheckHeartBeatIntervalTimeout)
        context.log.warn(s"心跳超时，状态转换为不可达:$peer")
        unreachable(peer)
      } else {
        Behaviors.same
      }
      //关闭链接
    case ServerTerminate =>
      peer.close()
      Behaviors.stopped
  }

  /**
   * 不可达状态
   *
   * @return
   */
  def unreachable(peer: Peer): Behavior[Message] = Behaviors.receiveMessage {
    //定时向服务器发送心跳请求
    case SendHeatBeat =>
      context.log.warn(s"服务端无法链接，正在心跳请求：$peer")
      dispatcher ! HeartBeat(IDGenerator.next(),option.appName,peer,heartBeatenAdapter)
      Behaviors.same
    case WrappedHeartBeaten(response) if response.isInstanceOf[HeartBeaten]=>
      //将返回的服务器数据交给连接管理器处理
      manager ! ServerActive(node.uri())
      up(Deadline.now.time, peer)
    case ServerTerminate =>
      peer.close()
      Behaviors.stopped
  }

  /**
   * 将hosts转换为节点集合
   *
   * @param hosts
   * @return
   */
  implicit def node(hosts: Option[String]): Option[Set[Node]] = {
    // TODO: 需要测试边缘情况，譬如只有一个server时
    hosts match {
      case Some(value) => {
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
}
