package com.zhongfei.scheduler.network

import akka.actor.typed.scaladsl.{ActorContext, Behaviors, TimerScheduler}
import akka.actor.typed.{ActorRef, Behavior}
import com.zhongfei.scheduler.network.ApplicationDispatcher.{Command, Done, Retry, Timeout}
import com.zhongfei.scheduler.network.ServerDispatcher.{HeartBeaten, Unregistered}
import com.zhongfei.scheduler.options.SingletonOption
import com.zhongfei.scheduler.transport.Peer
import com.zhongfei.scheduler.transport.protocol.SchedulerProtocol.{ActionTypeEnum, Response}
import io.netty.channel.ChannelFuture

/**
 * 应用分发处理器
 *
 */
// TODO: 需要设置监督策略，失败时打印消息 ？？？
object ApplicationDispatcher {


  trait Command

  case class Done(success: Boolean, retryCount: Int, cause: Throwable, message: Response) extends Command

  case class Retry(done: Done) extends Command

  case object Timeout extends Command

  def apply(option: SingletonOption, peer: Peer, message: ServerDispatcher.Command[_]): Behavior[Command] = Behaviors.setup { context =>
    Behaviors.withTimers { timer =>
      new ApplicationDispatcher(option, peer, timer, message, context).process()
    }
  }
}

/**
 * 处理应用管理器相关事务,临时对象,这个对象保存状态
 */
private class ApplicationDispatcher(option: SingletonOption, peer: Peer, timers: TimerScheduler[Command], message: ServerDispatcher.Command[_], context: ActorContext[Command]) {
  timers.startSingleTimer(Timeout, Timeout, option.processWaitTime)

  def process(): Behavior[Command] = Behaviors.receiveMessage[Command] {
    //心跳返回消息
    case HeartBeaten(actionId) =>
      val response = Response(actionId = actionId, actionType = ActionTypeEnum.HeartBeat.id.toByte)
      context.log.info(s"发送应用心跳响应，actionId=$actionId")
      send(option.transferRetryCount, response, context.self)
      Behaviors.same
    //处理应用取消注册请求，创建回复信息，并回复
    case Unregistered(actionId) =>
      val response = Response(actionId = actionId, actionType = ActionTypeEnum.Unregister.id.toByte)
      send(option.transferRetryCount, response, context.self)
      //返回取消注册响应消息
      context.log.info(s"应用取消注册完成，关闭actor，actionId=$actionId")
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
        context.log.error(s"响应消息发送失败,消息：$message", done.cause)
        Behaviors.stopped
      }
    case retry: Retry =>
      context.log.error(s"响应消息发送失败,进行重试，剩余次数：${retry.done.retryCount},消息体：$message")
      send(retry.done.retryCount, retry.done.message, context.self)
      Behaviors.same
  }

  def send(retryCount: Int, response: Response, actorRef: ActorRef[Command]): Unit = {
    peer.channel.writeAndFlush(response).addListener((future: ChannelFuture) => {
      actorRef ! Done(future.isSuccess, retryCount, future.cause(), response)
    })
  }
}
