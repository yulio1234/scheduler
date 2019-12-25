package com.zhongfei.scheduler.transfer

import akka.actor.typed.scaladsl.{ActorContext, Behaviors, TimerScheduler}
import akka.actor.typed.{ActorRef, Behavior}
import com.zhongfei.scheduler.network.ServerDispatcher
import com.zhongfei.scheduler.options.ServerOption
import com.zhongfei.scheduler.transfer.Transfer.{Command, Done, Retry, Timeout}
import com.zhongfei.scheduler.transport.Peer
import io.netty.channel.ChannelFuture

/**
 * 应用分发处理器
 *
 */
// TODO: 需要设置监督策略，失败时打印消息 ？？？
object Transfer {


  trait Command

  case class Done(success: Boolean, retryCount: Int, cause: Throwable, message: Command) extends Command

  case class Retry(done: Done) extends Command

  case object Timeout extends Command

  def apply(option: ServerOption, peer: Peer, message: ServerDispatcher.Command): Behavior[Command] = Behaviors.setup { context =>
    Behaviors.withTimers { timer =>
      new Transfer(option, peer, timer, message, context).process()
    }
  }
}

/**
 * 处理应用管理器相关事务,临时对象,这个对象保存状态
 */
private class Transfer(option: ServerOption, peer: Peer, timers: TimerScheduler[Command], command: ServerDispatcher.Command, context: ActorContext[Command]) {
  timers.startSingleTimer(Timeout, Timeout, option.processWaitTime)

  def process(): Behavior[Command] = Behaviors.receiveMessage[Command] { message =>
    val self = context.self
    message match {
      case message if message.isInstanceOf[OperationResult] =>
        context.log.info(s"发送操作响应，actionId=$command")
        send(option.transferRetryCount, message, self)
        Behaviors.same
      case Timeout =>
        context.log.error("应用处理超时,关闭应用分发器")
        Behaviors.stopped
      case done: Done if done.success =>
        context.log.debug("响应消息发送成功，关闭actor")
        Behaviors.stopped
      case done: Done if done.success =>
        //如果重试次数大于0，就重试
        if (done.retryCount > 0) {
          timers.startSingleTimer(Retry, Retry(done.copy(retryCount = done.retryCount - 1)), option.transferRetryInterval)
          Behaviors.same
        } else {
          context.log.error(s"响应消息发送失败,消息：$command", done.cause)
          Behaviors.stopped
        }
      case retry: Retry =>
        context.log.error(s"响应消息发送失败,进行重试，剩余次数：${retry.done.retryCount},消息体：$command")
        send(retry.done.retryCount, retry.done.message, self)
        Behaviors.same
    }
  }

  def send(retryCount: Int, message: Command, actorRef: ActorRef[Command]): Unit = {
    peer.channel.writeAndFlush(message).addListener((future: ChannelFuture) => {
      actorRef ! Done(future.isSuccess, option.transferRetryCount, future.cause(), message)
    })
  }
}
