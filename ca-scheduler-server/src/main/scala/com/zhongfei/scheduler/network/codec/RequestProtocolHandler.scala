package com.zhongfei.scheduler.network.codec

import akka.actor.typed.ActorRef
import com.zhongfei.scheduler.network.ServerDispatcher.Command
import com.zhongfei.scheduler.transport.codec.ProtocolHandler
import com.zhongfei.scheduler.transport.protocol.SchedulerProtocol.Request
import com.zhongfei.scheduler.utils.Logging
import io.netty.channel.Channel

object  RequestProtocolHandler{
  def apply(actor: ActorRef[Command]): RequestProtocolHandler = new RequestProtocolHandler(actor)
}
/**
 * 请求消息处理器
 * @param actor
 */
class RequestProtocolHandler(actor:ActorRef[Command]) extends ProtocolHandler[Request,Command] with Logging{
  /**
   *
   * @param message
   * @param channel
   */
  override def doHandler(message: Request, channel: Channel): Unit = {
    debug(s"请求处理器处理请求 message=$message")
    handle(message.actionType,message,actor,channel)
  }
}
