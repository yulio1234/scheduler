package com.zhongfei.scheduler.registry

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext}
import com.zhongfei.scheduler.registry.Repeater.{Command, Reply}
import com.zhongfei.scheduler.transport.protocol.SchedulerProtocol.Response
import io.netty.channel.{Channel, ChannelFuture}
object Repeater{
  trait Command
  case class Reply(response: Response,replyTo:Channel)
  case class Done()
//  def apply(): Repeater = new Repeater(context)

}

/**
 * 转发器，专门处理远程通讯
 */
class Repeater(context:ActorContext[Command]) extends AbstractBehavior[Repeater.Command](context){
  override def onMessage(msg: Command): Behavior[Command] = {
    case Reply(response,replyTo) =>
      replyTo.writeAndFlush(response).addListener((future: ChannelFuture) => {
//        future.isSuccess match {
//          case false => throw  RepeaterChannelWriteException()
//        }
      })
  }
}
