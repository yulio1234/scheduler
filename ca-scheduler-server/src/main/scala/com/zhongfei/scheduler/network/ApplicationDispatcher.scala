package com.zhongfei.scheduler.network

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, TimerScheduler}
import com.zhongfei.scheduler.command.SchedulerCommand.{HeartBeaten, Unregistered}
import com.zhongfei.scheduler.network.ApplicationDispatcher.{Message, Timeout}
import com.zhongfei.scheduler.options.SingletonOption
import com.zhongfei.scheduler.transport.NettyTransfer.WrappedResponse
import com.zhongfei.scheduler.transport.protocol.SchedulerProtocol.Response
import com.zhongfei.scheduler.transport.{NettyTransfer, Peer}

/**
 * 应用分发处理器
 *
 */
// TODO: 需要设置监督策略，失败时打印消息
object ApplicationDispatcher{
  trait Message
  trait Command extends Message
  trait Event extends Message
  case object Timeout extends Command
  def apply(option:SingletonOption, peer: Peer, message: ServerDispatcher.Command): Behavior[Message] = Behaviors.setup{ context =>
    Behaviors.withTimers{timer =>
      new ApplicationDispatcher(option,peer,timer,message,context).process()
    }
  }
}
/**
 * 处理应用管理器相关事务,临时对象,这个对象保存状态
 */
private class ApplicationDispatcher(option:SingletonOption, peer: Peer, timers: TimerScheduler[Message], message:ServerDispatcher.Command, context:ActorContext[Message]) {
  timers.startSingleTimer(Timeout,Timeout,option.processWaitTime)
  def process(): Behavior[Message] = Behaviors.receiveMessage[Message]{ message =>
    message match {
        //心跳返回消息
      case HeartBeaten(actionId,hosts) =>
        //如果有返回的地址数据，就将放回数据发回去
        hosts match {
          case Some(value) =>
            val bytes:Array[Byte] = value.getBytes
            val response = Response(actionId = actionId, length = bytes.length, content = bytes)
            send(WrappedResponse(response))
            Behaviors.same
          case None =>
            val response = Response(actionId = actionId)
            send(WrappedResponse(response))
            Behaviors.same
        }
        //处理应用取消注册请求，创建回复信息，并回复
      case Unregistered(actionId) =>
        val response = Response(actionId = actionId)
        //返回取消注册响应消息
        send(WrappedResponse(response))
        context.log.info(s"应用取消注册完成，关闭actor，actionId=$actionId")
        Behaviors.stopped
      case Timeout =>
        context.log.error("应用处理超时,关闭当前事务")
        Behaviors.stopped
    }
  }
  def send(wrappedResponse: WrappedResponse): Unit ={
    val transfer : ActorRef[NettyTransfer.Command] = context.spawn(NettyTransfer(peer.channel, option.transferRetryCount, option.transferRetryInterval), s"application-${peer.uri()}-transfer")
    transfer ! wrappedResponse
  }
}
