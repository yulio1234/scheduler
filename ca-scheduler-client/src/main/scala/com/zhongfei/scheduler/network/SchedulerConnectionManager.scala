package com.zhongfei.scheduler.network

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import com.zhongfei.scheduler.network.SchedulerConnectionManager.{Command, Message, Register, ServerTerminated}
import com.zhongfei.scheduler.transport.{Node, Peer}

object SchedulerConnectionManager {
  trait Message
  trait Event extends Message
  trait Command extends Message
  case object ServerUp extends Command
  case class ServerUpped(serverKey:String) extends Event

  case class Register(nodes: Array[Node]) extends Command with SchedulerConnection.Command
  case class ServerTerminated(serverKey:String) extends Command
  case class Registered()
  case class ActiveServer(hosts: Option[String]) extends Event

  def apply(option: ClientOption): Behavior[Message] = Behaviors.setup { context =>
    new SchedulerConnectionManager(option,context).handle(Map.empty)
  }
}

/**
 * 只负责管理连接，不负责处理各种事件
 * @param option
 * @param context
 */
class SchedulerConnectionManager(option: ClientOption,context: ActorContext[Message]) {
  def handle(waitOnlineServer:Map[String,Node],onlineServer: Map[String, ActorRef[SchedulerConnection.Command]]): Behavior[Message] = Behaviors.receiveMessage { message =>
    message match {
      case register @ Register(nodes) =>
        // TODO: 状态机。。。
        Behaviors.same
        //注销服务
      case ServerTerminated(serverKey) =>
        handle(waitOnlineServer,onlineServer - serverKey)
    }
  }

  /**
   * 每次心跳服务器返回可以用服务列表，比较可以服务列表和在线服务列表，如果有不在线的，就更新为最新的，并尝试链接。
   * 至于服务器返回的在线列表是否一致，服务器采用共识算法（quorum）保证每次数据返回的正确性
   * @param waitOnlineServer
   */
  def tryOnline(waitOnlineServer:Map[String,Node]): Unit ={

  }
  {
    onlineServer.get(peer.uri()) match {
      //如果有就不注册
      case Some(server) =>
        server ! register
        Behaviors.same
      //如果没有就新增
      case None =>
        val server = context.spawn(SchedulerConnection(option, peer,context.self), s"server-${peer.uri()}")
        context.watchWith(server,ServerTerminated(peer.uri()))
        handle(serverMap + (peer.uri() -> server))
    }
  }
}
