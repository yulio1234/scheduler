package com.zhongfei.scheduler.network.codec

import akka.actor.typed.ActorRef
import com.zhongfei.scheduler.network.Dispatcher.Message
import com.zhongfei.scheduler.transport.codec.ProtocolHandlerFactory
import com.zhongfei.scheduler.transport.protocol.SchedulerProtocol.ActionTypeEnum

/**
 * 请求协议处理器简单工厂
 */
object ResponseProtocolHandlerFactory extends ProtocolHandlerFactory[Message,ResponseProtocolHandler]{

  override def create(actor:ActorRef[Message]): ResponseProtocolHandler ={
    val handler = new ResponseProtocolHandler(actor)
    //注册心跳解码处理器
    handler.registerCommand(ActionTypeEnum.HeartBeat.id,new HeartBeatenDecoder)
    handler.registerCommand(ActionTypeEnum.Unregister.id,new UnregisteredDecoder)
    handler.registerCommand(ActionTypeEnum.ScheduleAdd.id,new ScheduleAddedDecoder)
    handler
  }

}
