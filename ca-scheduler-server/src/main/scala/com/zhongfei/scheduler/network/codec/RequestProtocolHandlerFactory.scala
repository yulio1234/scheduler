package com.zhongfei.scheduler.network.codec

import akka.actor.typed.ActorRef
import com.zhongfei.scheduler.network.ServerDispatcher.Command
import com.zhongfei.scheduler.transport.codec.ProtocolHandlerFactory
import com.zhongfei.scheduler.transport.protocol.SchedulerProtocol.ActionTypeEnum

/**
 * 请求协议处理器简单工厂
 */
object RequestProtocolHandlerFactory extends ProtocolHandlerFactory{

  def create(actor:ActorRef[Command]): RequestProtocolHandler ={
    val handler = new RequestProtocolHandler(actor)
    //注册心跳解码处理器
    handler.registerCommand(ActionTypeEnum.HeartBeat.id,new HeartBeatDecoder)
    handler
  }

}
