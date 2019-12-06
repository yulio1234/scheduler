package com.zhongfei.scheduler.registry

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, TimerScheduler}
import com.zhongfei.scheduler.registry.Application.Command
import com.zhongfei.scheduler.transport.Peer

object Application{
  trait Command

  def apply(peer: Peer): Behavior[Command] = Behaviors.setup{context => Behaviors.withTimers{timers => new Application(peer,timers,context).handle()}}
}

/**
 * 应用处理器，专门处理应用相关消息
 * @param peer 对等体，代表通信的另一端
 * @param timers 调度器
 * @param context actor上下文
 */
private class Application(peer: Peer, timers: TimerScheduler[Command], context:ActorContext[Application.Command]){

  private def handle(): Behavior[Application.Command] = Behaviors.receiveMessage{message =>{
    Behaviors.same
  }}
}
