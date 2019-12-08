package com.zhongfei.scheduler.network

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import com.zhongfei.scheduler.network.ServerManager.{Command, Register}
import com.zhongfei.scheduler.transport.Peer

object ServerManager{
  trait Command
  case class Register(peer: Peer) extends Server.Command

}
class ServerManager {
  def handle(serverMap:Map[String,ActorRef[Server.Command]],context:ActorContext[Command]):Behavior[Command] = Behaviors.receiveMessage{ message =>
    message match {
      case  Register(peer) =>
        serverMap.get(peer.uri()) match {
            //如果有就不注册
          case Some(server) => Behaviors.unhandled
            //如果没有就新增
          case None =>
            context.spawn(Server())
        }
    }
    Behaviors.same
  }
}
