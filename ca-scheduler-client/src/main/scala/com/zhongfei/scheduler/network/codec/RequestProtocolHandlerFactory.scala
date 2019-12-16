package com.zhongfei.scheduler.network.codec

import akka.actor.typed.ActorRef
import com.zhongfei.scheduler.network.Dispatcher.Message
import com.zhongfei.scheduler.transport.codec.ProtocolHandlerFactory

/**
 * 请求处理器工厂
 */
object RequestProtocolHandlerFactory extends ProtocolHandlerFactory[Message,RequestProtocolHandler]{
  override def create(actor: ActorRef[Message]): RequestProtocolHandler = {
    null
  }
}
