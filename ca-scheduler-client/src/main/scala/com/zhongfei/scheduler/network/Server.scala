package com.zhongfei.scheduler.network

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, TimerScheduler}
import com.zhongfei.scheduler.network.Server.Command
import com.zhongfei.scheduler.network.ServerManager.Register
import com.zhongfei.scheduler.transport.Peer

object Server{
  trait Command

  def apply(peer: Peer,option: ClientOption): Behavior[Command] = Behaviors.setup{context => Behaviors.withTimers{timers => new Server(peer,option,timers,context).handle()}}
}
class Server(peer: Peer,option: ClientOption,timers: TimerScheduler[Command],context:ActorContext[Command]) {
  def handle():Behavior[Command] = Behaviors.receiveMessage{ message =>
    message match {
      case  Register(peer)=>

    }
    Behaviors.same
  }
}
