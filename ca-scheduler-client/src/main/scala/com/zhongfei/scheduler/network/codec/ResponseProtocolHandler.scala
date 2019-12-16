package com.zhongfei.scheduler.network.codec

import akka.actor.typed.ActorRef
import com.zhongfei.scheduler.network.Dispatcher.Message
import com.zhongfei.scheduler.transport.codec.ProtocolHandler
import com.zhongfei.scheduler.transport.protocol.SchedulerProtocol.Response
import io.netty.channel.Channel

/**
 * 响应协议处理器
 */
class ResponseProtocolHandler(actor:ActorRef[Message])  extends ProtocolHandler[Response,Message]{
  override def doHandler(message: Response, channel: Channel): Unit = {
    handle(message.actionType,message,actor,channel)
  }
}