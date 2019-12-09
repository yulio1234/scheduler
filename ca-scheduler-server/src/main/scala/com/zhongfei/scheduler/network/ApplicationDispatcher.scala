package com.zhongfei.scheduler.network

import akka.actor.typed.scaladsl.{ActorContext, Behaviors, TimerScheduler}
import akka.actor.typed.{ActorRef, Behavior}
import com.zhongfei.scheduler.command.SchedulerCommand.{HeartBeat, Unregister, Unregistered}
import com.zhongfei.scheduler.network.ApplicationDispatcher.{Command, Timeout}
import com.zhongfei.scheduler.network.CoreDispatcher.ProtocolCommand
import com.zhongfei.scheduler.options.SingletonOption
import com.zhongfei.scheduler.transport.NettyTransfer.WrappedResponse
import com.zhongfei.scheduler.transport.protocol.SchedulerProtocol.{ActionTypeEnum, Request, Response}
import com.zhongfei.scheduler.transport.{NettyTransfer, Peer}

/**
 *
 */
object ApplicationDispatcher{
  trait Command
  case object Timeout extends Command
  trait Event
  def apply(option:SingletonOption, peer: Peer, replyTo: ActorRef[CoreDispatcher.Command], transferTo: ActorRef[ApplicationManager.Command]): Behavior[Command] = Behaviors.setup{ context =>
    Behaviors.withTimers{timer =>
      new ApplicationDispatcher(option,peer,timer,replyTo,transferTo,context).process()
    }
  }
}
/**
 * 处理应用管理器相关事务,临时对象,这个对象保存状态
 */
private class ApplicationDispatcher(option:SingletonOption, peer: Peer, timers: TimerScheduler[Command], replyTo:ActorRef[CoreDispatcher.Command], transferTo:ActorRef[ApplicationManager.Command], context:ActorContext[Command]) {
  timers.startSingleTimer(Timeout,Timeout,option.processWaitTime)
  def process(): Behavior[Command] = Behaviors.receiveMessage[Command]{ message =>
    message match {
        //处理协议请求
      case ProtocolCommand(peer,protocol) =>
        protocol match {
          //处理请求消息
          case Request(_, _, _, actionId, actionType, _, _, _,appName)=>
            if(actionType == ActionTypeEnum.HeartBeat.id) {
              transferTo ! HeartBeat(actionId,new String(appName),peer)}
            else if(actionType == ActionTypeEnum.Unregister.id) {
              transferTo ! Unregister(actionId,new String(appName),peer,context.self)
            }
            Behaviors.same
        }
        //处理应用取消注册请求，创建回复信息，并回复
      case Unregistered(actionId) =>
        val response = Response(actionId = actionId)
        //创建一个临时通讯对象，进行通讯
        val transfer = context.spawnAnonymous(NettyTransfer(peer.channel, option.transferRetryCount, option.transferRetryInterval))
        transfer ! WrappedResponse(response)
        context.log.info(s"应用取消注册完成，关闭actor，actionId=$actionId")
        Behaviors.stopped
      case Timeout =>
        context.log.error("应用处理超时,关闭当前事务")
        Behaviors.stopped

    }
  }
}
