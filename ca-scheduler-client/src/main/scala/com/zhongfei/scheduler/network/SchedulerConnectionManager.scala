package com.zhongfei.scheduler.network

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import com.zhongfei.scheduler.network.SchedulerConnectionManager._
import com.zhongfei.scheduler.transport.Node

object SchedulerConnectionManager {

  trait Message

  trait Event extends Message

  trait Command extends Message

  //连接服务
  case class Connects(nodes: Array[Node]) extends Command with SchedulerConnection.Command

  //连接服务完成
  case class Connected(serverKey: String, serverRef: ActorRef[SchedulerConnection.Command])

  //不可达命令
  case class ServerUnreachable(serverKey:String) extends Command with SchedulerConnection.Command

  case class ServerTerminated(serverKey: String) extends Command

  case class ActiveServer(hosts: Option[Set[Node]]) extends Event

  def apply(option: ClientOption): Behavior[Message] = Behaviors.setup { context =>
    new SchedulerConnectionManager(option, context).down()
  }
}

/**
 * 只负责管理连接，不负责处理各种事件
 *
 * @param option
 * @param context
 */
class SchedulerConnectionManager(option: ClientOption, context: ActorContext[Message]) {
  /**
   * 下线状态
   * @return
   */
  def down(): Behavior[Message] = Behaviors.receiveMessage {
    case Connects(nodes) =>
      nodes.foreach(node =>
        context.spawn(SchedulerConnection(option,node,context.self),"server-"+node.uri())
      )
      Behaviors.same
    case Connected(serverKey,serverRef) =>
      up(Map.empty,Map.empty + (serverKey -> serverRef))

  }

  /**
   * 上线状态
   * @param waitOnlineServer 等待上下的应用
   * @param onlineServer
   * @return
   */
  def up(waitOnlineServer: Map[String, ActorRef[SchedulerConnection.Command]], onlineServer: Map[String, ActorRef[SchedulerConnection.Command]]): Behavior[Message] ={
        //活跃的服务
    case ActiveServer(hosts) =>
      hosts match {
        case Some(nodes) =>
          //当前活跃的服务
          val activeServerKeys = nodes.collect(node => node.uri())
          //本来活跃但是不再活跃的服务
          val offlineServerKeySet:Set[String] = onlineServer.keySet -- activeServerKeys
          //已经不活跃但是没再不活跃名单里的服务
          val waitOnlineServerKeySet = waitOnlineServer.keySet
          val set = offlineServerKeySet -- waitOnlineServerKeySet
          //把不再活跃的服务放进等待上线的集合里，并发送不可达命令
          val newWaitOnlineServer = onlineServer -- set
        case None =>
      }
      //接收服务发来的不可达消息
    case ServerUnreachable(serverKey) =>

    case ServerTerminated(serverKey) =>
      up(waitOnlineServer,onlineServer - serverKey)

  }

  /**
   * 每次心跳服务器返回可以用服务列表，比较可以服务列表和在线服务列表，如果有不在线的，就更新为最新的，并尝试链接。
   * 至于服务器返回的在线列表是否一致，服务器采用共识算法（quorum）保证每次数据返回的正确性
   *
   * @param waitOnlineServer
   */
  def tryConnect(waitOnlineServer: Map[String, Node]): Unit = {

  }
  {
    onlineServer.get(peer.uri()) match {
      //如果有就不注册
      case Some(server) =>
        server ! register
        Behaviors.same
      //如果没有就新增
      case None =>
        val server = context.spawn(SchedulerConnection(option, peer, context.self), s"server-${peer.uri()}")
        context.watchWith(server, ServerTerminated(peer.uri()))
        handle(serverMap + (peer.uri() -> server))
    }
  }
}
