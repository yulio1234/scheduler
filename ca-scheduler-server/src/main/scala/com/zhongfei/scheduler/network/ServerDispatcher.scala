package com.zhongfei.scheduler.network

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import com.zhongfei.scheduler.command.SchedulerCommand
import com.zhongfei.scheduler.command.SchedulerCommand.{HeartBeat, Unregister}
import com.zhongfei.scheduler.network.ApplicationDispatcher.Event
import com.zhongfei.scheduler.network.ServerDispatcher.{Command, Message}
import com.zhongfei.scheduler.options.SingletonOption
import com.zhongfei.scheduler.transport.Peer

/**
 * 调度处理器处理器
 */
object ServerDispatcher {
  trait Message
  trait Command extends Message
  trait Event extends Message
  //与应用通讯的消息的消息
  def apply(option: SingletonOption): Behavior[Message] = Behaviors.setup{ context => new ServerDispatcher(option,context).process()}
}

/**
 * 全局消息处理器，负责分发各种消息
 * @param option
 */
private class ServerDispatcher(option:SingletonOption, context:ActorContext[Message])  {
  //创建应用管理者
  val applicationManager = context.spawn(ApplicationManager(option),"applicationManager")

  // TODO:  进行查询数据保存
  def process(): Behavior[Message] = Behaviors.receiveMessage[Message]{message => {
    message match {
      case heartbeat @ HeartBeat(_, _, peer:Peer,_) =>
        val actorRef: ActorRef[Event] = buildExtra(message, peer)
        //转发给应用管理器
        applicationManager ! heartbeat.copy(replyTo = actorRef)
        Behaviors.same
        //匹配注销应用请求，并转发
      case unregister @ Unregister(_,_,peer,_) =>
        val actorRef = buildExtra(message, peer)
        applicationManager ! unregister.copy(reply = actorRef)
        Behaviors.same
    }
  }}
  def buildExtra(message:Message,peer: Peer): ActorRef[ApplicationDispatcher.Event] ={
    context.spawnAnonymous(ApplicationDispatcher(option, peer, message))
  }
}
