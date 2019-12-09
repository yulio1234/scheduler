package com.zhongfei.scheduler.network

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import com.zhongfei.scheduler.network.ServerManager.{Command, Register, ServerTerminated}
import com.zhongfei.scheduler.transport.Peer

object ServerManager {

  trait Command

  case class Register(peer: Peer) extends Command with Server.Command

  case class ServerTerminated(serverKey:String) extends Command

  def apply(option: ClientOption): Behavior[Command] = Behaviors.setup { context =>
    new ServerManager(option,context).handle(Map.empty)
  }
}

class ServerManager(option: ClientOption,context: ActorContext[Command]) {
  def handle(serverMap: Map[String, ActorRef[Server.Command]]): Behavior[Command] = Behaviors.receiveMessage { message =>
    message match {
      case register @ Register(peer) =>
        serverMap.get(peer.uri()) match {
          //如果有就不注册
          case Some(server) =>
            server ! register
            Behaviors.same
          //如果没有就新增
          case None =>
            val server = context.spawn(Server(option, peer), s"server-${peer.uri()}")
            context.watchWith(server,ServerTerminated(peer.uri()))
            handle(serverMap + (peer.uri() -> server))
        }

    }
  }
}
