package com.zhongfei.scheduler.network.codec

import java.util

import com.zhongfei.scheduler.network.ApplicationManager.Unregistered
import com.zhongfei.scheduler.transport.protocol.SchedulerProtocol.{ActionTypeEnum, Response}
import com.zhongfei.scheduler.utils.Logging
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToMessageEncoder

class UnregisteredToResponseEncoder extends MessageToMessageEncoder[Unregistered] with Logging{
  override def encode(ctx: ChannelHandlerContext, msg: Unregistered, out: util.List[AnyRef]): Unit = {
    debug(s"注销应用编码处理器接收到消息：$msg")
    val response = Response(actionId = msg.actionId, actionType = ActionTypeEnum.Unregister.id.toByte)
    out.add(response)
  }
}
