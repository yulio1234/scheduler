package com.zhongfei.scheduler.transport

import akka.actor.typed.scaladsl.{ActorContext, Behaviors, TimerScheduler}
import akka.actor.typed.{Behavior, PostStop}
import com.zhongfei.scheduler.Processor
import com.zhongfei.scheduler.transport.NettyTransfer.{Command, Failure, Success, WrappedResponse}
import com.zhongfei.scheduler.transport.SchedulerExceptions.NetworkTransferException
import com.zhongfei.scheduler.transport.protocol.SchedulerProtocol.{Request, Response}
import io.netty.channel.{Channel, ChannelFuture}

import scala.concurrent.duration.FiniteDuration
object NettyTransfer{
  trait Command
  trait Event
  case class Retry(count:Int)
  //处理成功
  case object Success extends Command
  //处理失败
  case class Failure(cause:Throwable) extends Command
  case class WrappedResponse(response: Response) extends Command
  case class WrappedRequest(request: Request) extends Command
  def apply(channel: Channel,retryCount:Int,interval: FiniteDuration,closeChannel:Boolean = false): Behavior[Command] = Behaviors.setup{context =>
        Behaviors.withTimers{timers => new NettyTransfer(channel, timers, interval, context,closeChannel).process(retryCount)}}
}

/**
 * 传输器，专门处理远程通讯
 */
// TODO: 试试用高等函数来写
class NettyTransfer(channel: Channel,
                    timers: TimerScheduler[Command],
                    interval: FiniteDuration,
                    context:ActorContext[Command],
                    closeChannel:Boolean = false) extends Processor[Int,Behavior[Command]]{
  override def process(retryCount: Int): Behavior[Command] = {
    Behaviors.receiveMessage[Command] { message =>
      message match {
        //处理取消注册请求
        case wrappedResponse: WrappedResponse =>
          //进行远程通信
          send(wrappedResponse.response, context);
        case Success =>
          context.log.info("数据传输成功")
          Behaviors.stopped
        case Failure(cause) =>
          context.log.error("netty数据传输异常", cause)
          retryCount match {
            //重试次数大于0就继续重试
            case n if (n > 0) =>
              context.log.error(s"当前重试次数剩余：$retryCount 次，进行重试")
              timers.startSingleTimer(message, message, interval)
            //如果重试次数不够了，就回复给发送者，并停止
            case _ => throw new NetworkTransferException(cause)
          }
      }
      //重试次数减一
      process(retryCount - 1)
    }.receiveSignal {
          //如果通讯后关闭channel为true，就关闭
      case (_, PostStop) =>
        if (closeChannel) {
          channel.close()
        }
        Behaviors.same
    }
  }

  def send(response: Response,context:ActorContext[Command])={
    val self = context.self
    channel.writeAndFlush(response).addListener((future: ChannelFuture) => {
      future.isSuccess match {
        case true =>
          //回复给自己已经成功
          self ! Success
        //回复给自己失败了
        case false => Failure(future.cause())
      }
    })
  }
}
