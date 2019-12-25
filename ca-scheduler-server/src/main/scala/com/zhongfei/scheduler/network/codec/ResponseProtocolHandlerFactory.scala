package com.zhongfei.scheduler.network.codec

import akka.actor.typed.ActorRef
import com.zhongfei.scheduler.network.ServerDispatcher.Command
import com.zhongfei.scheduler.transport.codec.ProtocolHandlerFactory

/**
 * 响应处理器
 */
object ResponseProtocolHandlerFactory extends ProtocolHandlerFactory[Command,ResponseProtocolHandler]{
  override def create(actor: ActorRef[Command]): ResponseProtocolHandler = {
    new ResponseProtocolHandler(actor)
  }
}
