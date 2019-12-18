package com.zhongfei.scheduler.network

import akka.actor.typed.scaladsl.{ActorContext, Behaviors, TimerScheduler}
import akka.actor.typed.{ActorRef, Behavior}
import com.zhongfei.scheduler.network.Dispatcher.{HeartBeat, HeartBeaten, Unregister}
import com.zhongfei.scheduler.network.SchedulerConnection._
import com.zhongfei.scheduler.network.SchedulerConnectionManager.{Event => _, Message => _, apply => _, _}
import com.zhongfei.scheduler.transport.{Node, Peer}
import com.zhongfei.scheduler.utils.IDGenerator
import io.netty.channel.ChannelFuture

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

  case object Unreachable extends Command

  def apply(option: ClientOption,
            node: Node,
            manager: ActorRef[SchedulerConnectionManager.Message],
            dispatcher: ActorRef[Dispatcher.Message],
            schedulerClient: SchedulerClient): Behavior[Message] =
    Behaviors.setup { context =>
      Behaviors.withTimers { timers =>
        new SchedulerConnection(option, node, timers, manager, dispatcher, schedulerClient, context).down()
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
                          client: SchedulerClient, context: ActorContext[Message]) {

  case class WrappedHeartBeaten(response: Dispatcher.Message) extends Event

  private val heartBeatenAdapter: ActorRef[Dispatcher.Message] = context.messageAdapter(WrappedHeartBeaten)

  /**
   * 被移除的状态，
   * @return
   */
  def removed():Behavior[Message] = Behaviors.receiveMessage{
    case Reconnect =>
      context.log.debug(s"服务器节点：$node，服务已经被移除，不执行重连请求操作")
      Behaviors.stopped
    case ? @ _ =>
      context.log.warn(s"服务器节点：$node，关闭状态接收到请求 $?")
      Behaviors.stopped
  }
  /**
   * 开始为下线状态
   *
   * @return
   */
  def down(): Behavior[Message] = Behaviors.receiveMessage {
    //接收初始化消息,执行初始化
    case Initialize =>
      context.log.debug(s"服务器节点：$node，接收到初始化请求")
      //创建超时消息
      timers.startSingleTimer(InitializeTimeout, InitializeTimeout, option.iniTimout)
      val self = context.self
      //创建客户端
      client.createConnection(node).addListener { (future: ChannelFuture) =>
        //返回初始化结果
        context.log.debug(s"服务器节点：$node，注册初始化响应监听")
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
        dispatcher ! HeartBeat(IDGenerator.next(), option.appName, peer, heartBeatenAdapter)
        //并通知管理器服务已经上线
        manager ! Connected(node.uri(), context.self)
        context.log.debug(s"服务器节点：$node，链接成功，转换为上线状态")
        timers.cancel(Reconnect)
        context.log.debug(s"服务器节点：$node，注册关闭链接监听")
        peer.channel.closeFuture().addListener { (future: ChannelFuture) =>
          if (future.isSuccess) {
            context.log.warn(s"服务器节点：$node，链接中断，状态转换为不可达")
            context.self ! Reconnect
          }
        }
        //转移到上线状态
        up(Deadline.now.time, peer)
      } else {
        //如果没连接上不可达
        //告诉管理器不可达
        manager ! ServerUnreachable(node.uri())
        //尝试重连
        timers.startSingleTimer(Reconnect, Reconnect, option.reconnectInterval)
        context.log.warn(s"服务器节点：$node，网络链接建立失败，无法与服务端通讯")
        Behaviors.same
      }
    //如果创建一直没回应，也重连
    case InitializeTimeout =>
      manager ! ServerUnreachable(node.uri())
      context.self ! Initialize
      Behaviors.same
    case Reconnect =>
      context.self ! Initialize
      Behaviors.same
    case ServerTerminate =>
      context.log.debug(s"服务器节点：$node，接收到关闭服务请求，正在关闭服务")
      removed()
  }

  def up(lastedHeartBeatTime: FiniteDuration, peer: Peer): Behavior[Message] = Behaviors.receiveMessage {
    //定时向服务器发送心跳请求
    case SendHeatBeat =>
      context.log.debug(s"服务器节点：$node，服务已在线，发送心跳请求")
      dispatcher ! HeartBeat(IDGenerator.next(), option.appName, peer, heartBeatenAdapter)
      Behaviors.same
    //收到心跳请求后，处理
    case WrappedHeartBeaten(response) if response.isInstanceOf[HeartBeaten] =>
      val heartBeaten = response.asInstanceOf[HeartBeaten]
      if (heartBeaten.success) {
        context.log.debug(s"服务器节点：$node，收到成功心跳响应，刷新记录时间")
        //将返回的服务器数据交给连接管理器处理
        up(Deadline.now.time, peer)
      }else{
        context.log.warn(s"服务器节点：$node，收到失败心跳响应",heartBeaten.cause)
        Behaviors.same
      }
    //检查心跳超时
    case CheckHeartBeatIntervalTimeout =>
      context.log.debug(s"服务器节点：$node，超时检测，上次记录时间为：$lastedHeartBeatTime")
      //当前时间减去最后一次记录的时间如果大于时间间隔就将连接设置为不可达
      val interval = Deadline.now - lastedHeartBeatTime
      if (interval.time > option.checkHeartBeatOnCloseInterval) {
        context.log.debug(s"服务器节点：$node，心跳检测已经超时，发送不可达消息")
        //发送不可达消息
        context.self ! Unreachable(node.uri())
      }
      Behaviors.same
    case Unreachable(serverKey) =>
      context.log.warn(s"服务器节点：$node，接收到不可达消息 $serverKey")
      //给服务器发送不可达消息
      manager ! ServerUnreachable(serverKey)
      //关闭心跳检查
      timers.cancel(CheckHeartBeatIntervalTimeout)
      unreachable(peer)
      //链接中断事件，更改状态为下线，并执行重连
    case Reconnect =>
      timers.cancel(SendHeatBeat)
      timers.cancel(CheckHeartBeatIntervalTimeout)
      timers.startSingleTimer(Reconnect, Reconnect, option.reconnectInterval)
      down()
    //关闭链接
    case ServerTerminate =>
      context.log.debug(s"服务器节点：$node，接收到关闭服务请求，正在关闭服务")
      dispatcher ! Unregister(IDGenerator.next(),option.appName)
      removed()
  }

  /**
   * 不可达状态
   *
   * @return
   */
  def unreachable(peer: Peer): Behavior[Message] = Behaviors.receiveMessage {
    //定时向服务器发送心跳请求
    case SendHeatBeat =>
      context.log.warn(s"服务器节点：$node，服务端无法链接，正在心跳请求：$peer")
      dispatcher ! HeartBeat(IDGenerator.next(), option.appName, peer, heartBeatenAdapter)
      Behaviors.same
    case WrappedHeartBeaten(response) if response.isInstanceOf[HeartBeaten] =>
      context.log.debug(s"服务器节点：$node，服务端通讯恢复，重新设置为上线状态：$response")
      //将返回的服务器数据交给连接管理器处理
      manager ! ServerActive(node.uri())
      up(Deadline.now.time, peer)
    //链接中断事件，更改状态为下线，并执行重连
    case Reconnect =>
      timers.cancel(SendHeatBeat)
      timers.cancel(CheckHeartBeatIntervalTimeout)
      timers.startSingleTimer(Reconnect, Reconnect, option.reconnectInterval)
      down()
    case ServerTerminate =>
      context.log.debug(s"服务器节点：$node，接收到关闭服务请求，正处于不可达状态，直接关闭服务")
      peer.close()
      removed()
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
