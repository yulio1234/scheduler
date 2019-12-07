package com.zhongfei.scheduler.processor

import akka.actor.typed.scaladsl.{ActorContext, Behaviors, TimerScheduler}
import akka.actor.typed.{ActorRef, Behavior}
import com.zhongfei.scheduler.Processor
import com.zhongfei.scheduler.processor.NettyTransferProcessor.{Command, Failure, Retry, Success}
import com.zhongfei.scheduler.registry.ApplicationManager
import com.zhongfei.scheduler.registry.ApplicationManager.Unregistered
import com.zhongfei.scheduler.transport.SchedulerExceptions.TransferException
import com.zhongfei.scheduler.transport.protocol.SchedulerProtocol.Response
import io.netty.channel.{Channel, ChannelFuture}

import scala.concurrent.duration.FiniteDuration
object NettyTransferProcessor{
  trait Command
  case class Retry(count:Int)
  //处理成功
  case object Success extends Command
  //处理失败
  case class Failure(cause:Throwable) extends Command
}

/**
 * 传输器，专门处理远程通讯
 */
class NettyTransferProcessor(channel: Channel,replyTo:ActorRef[ApplicationManager.Command],timers: TimerScheduler[Command],interval: FiniteDuration,context:ActorContext[Command]) extends Processor[Int,Behavior[Command]]{
  override def process(count: Int): Behavior[Command] = Behaviors.receiveMessage{ message =>
    message match {
      case ApplicationManager.Unregister(actionId, appName, peer) =>
        val response = Response(actionId = actionId)
        val self = context.self
        channel.writeAndFlush(response).addListener((future: ChannelFuture) => {
          future.isSuccess match {
            case true =>
              replyTo ! Unregistered(actionId)
              self ! Success
            case false => Failure(future.cause())
          }
        })
      case Success =>
        context.log.info("数据传输成功")
        Behaviors.stopped
      case Failure(cause) =>
        context.log.error("netty数据传输异常",cause)
        count match {
          case n if(n > 0) =>
            context.log.error(s"当前重试次数剩余：$count 次，进行重试")
            timers.startSingleTimer(???,message,interval)
          case _ => replyTo ! TransferException()
            Behaviors.stopped
        }
    }
    process(count - 1)
  }
}
