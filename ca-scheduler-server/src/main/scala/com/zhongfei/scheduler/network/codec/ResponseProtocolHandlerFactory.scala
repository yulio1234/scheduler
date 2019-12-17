package com.zhongfei.scheduler.network.codec

import akka.actor.typed.ActorRef
import com.zhongfei.scheduler.network.ServerDispatcher.{Event, Message}
import com.zhongfei.scheduler.transport.codec.ProtocolHandlerFactory

/**
 * 响应处理器
 */
object ResponseProtocolHandlerFactory extends ProtocolHandlerFactory[Message,ResponseProtocolHandler]{
  override def create(actor: ActorRef[Message]): ResponseProtocolHandler = {
    new ResponseProtocolHandler(actor)
  }
}
