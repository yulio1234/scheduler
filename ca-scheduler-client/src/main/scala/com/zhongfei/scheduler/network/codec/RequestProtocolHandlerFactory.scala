package com.zhongfei.scheduler.network.codec

import akka.actor.typed.ActorRef
import com.zhongfei.scheduler.network.Dispatcher.Command
import com.zhongfei.scheduler.transport.codec.ProtocolHandlerFactory

/**
 * 请求处理器工厂
 */
object RequestProtocolHandlerFactory extends ProtocolHandlerFactory[Command,RequestProtocolHandler]{
  override def create(actor: ActorRef[Command]): RequestProtocolHandler = {
    null
  }
}
