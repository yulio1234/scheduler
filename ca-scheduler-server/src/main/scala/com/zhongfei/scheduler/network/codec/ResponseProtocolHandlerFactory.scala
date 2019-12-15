package com.zhongfei.scheduler.network.codec

import akka.actor.typed.ActorRef
import com.zhongfei.scheduler.network.ServerDispatcher.Event
import com.zhongfei.scheduler.transport.codec.ProtocolHandlerFactory

/**
 * 响应处理器
 */
object ResponseProtocolHandlerFactory extends ProtocolHandlerFactory[Event,ResponseProtocolHandler]{
  override def create(actor: ActorRef[Event]): ResponseProtocolHandler = {
    new ResponseProtocolHandler(actor)
  }
}
