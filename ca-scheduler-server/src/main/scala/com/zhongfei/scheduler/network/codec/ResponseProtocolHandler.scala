package com.zhongfei.scheduler.network.codec

import akka.actor.typed.ActorRef
import com.zhongfei.scheduler.network.ServerDispatcher.Event
import com.zhongfei.scheduler.transport.codec.ProtocolHandler
import com.zhongfei.scheduler.transport.protocol.SchedulerProtocol.Response
import com.zhongfei.scheduler.utils.Logging
import io.netty.channel.Channel

/**
 * 响应协议处理器
 */
class ResponseProtocolHandler(actor:ActorRef[Event])  extends ProtocolHandler[Response,Event] with Logging{
  override def doHandler(message: Response, channel: Channel): Unit = {
    debug(s"响应处理器处理响应请求 message=$message")
    handle(message.actionType,message,actor,channel)
  }
}