package com.zhongfei.scheduler.network

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, TimerScheduler}
import com.zhongfei.scheduler.network.Server.Command
import com.zhongfei.scheduler.network.ServerManager.Register
import com.zhongfei.scheduler.transport.Peer

import scala.concurrent.duration.{Deadline, FiniteDuration}

object Server{
  trait Command

  def apply(option: ClientOption,peer: Peer): Behavior[Command] = Behaviors.setup{context => Behaviors.withTimers{timers => new Server(option,peer,timers,context).handle(Deadline.now.time)}}
}
class Server(option: ClientOption,peer: Peer,timers: TimerScheduler[Command],context:ActorContext[Command]) {
  def handle(lastedHeartBeatTime:FiniteDuration):Behavior[Command] = Behaviors.receiveMessage{ message =>
    message match {
      case  Register(peer)=>


    }
    Behaviors.same
  }
}
