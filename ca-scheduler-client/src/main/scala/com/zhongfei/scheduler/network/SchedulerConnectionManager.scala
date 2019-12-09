package com.zhongfei.scheduler.network

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import com.zhongfei.scheduler.network.SchedulerConnectionManager.{Command, Register, ServerTerminated}
import com.zhongfei.scheduler.transport.Peer

object SchedulerConnectionManager {

  trait Command

  case class Register(peer: Peer) extends Command with SchedulerConnection.Command

  case class ServerTerminated(serverKey:String) extends Command

  def apply(option: ClientOption): Behavior[Command] = Behaviors.setup { context =>
    new SchedulerConnectionManager(option,context).handle(Map.empty)
  }
}

/**
 * 只负责管理连接，不负责处理各种事件
 * @param option
 * @param context
 */
class SchedulerConnectionManager(option: ClientOption,context: ActorContext[Command]) {
  def handle(serverMap: Map[String, ActorRef[SchedulerConnection.Command]]): Behavior[Command] = Behaviors.receiveMessage { message =>
    message match {
      case register @ Register(peer) =>
        serverMap.get(peer.uri()) match {
          //如果有就不注册
          case Some(server) =>
            server ! register
            Behaviors.same
          //如果没有就新增
          case None =>
            val server = context.spawn(SchedulerConnection(option, peer), s"server-${peer.uri()}")
            context.watchWith(server,ServerTerminated(peer.uri()))
            handle(serverMap + (peer.uri() -> server))
        }

    }
  }
}
