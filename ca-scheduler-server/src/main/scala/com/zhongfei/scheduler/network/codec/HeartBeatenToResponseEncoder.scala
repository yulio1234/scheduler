package com.zhongfei.scheduler.network.codec

import java.util

import com.zhongfei.scheduler.network.ApplicationManager.HeartBeaten
import com.zhongfei.scheduler.transport.protocol.SchedulerProtocol.{ActionTypeEnum, Response}
import com.zhongfei.scheduler.utils.Logging
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToMessageEncoder

class HeartBeatenToResponseEncoder extends MessageToMessageEncoder[HeartBeaten] with Logging{
  override def encode(ctx: ChannelHandlerContext, msg: HeartBeaten, out: util.List[AnyRef]): Unit = {
    debug(s"心跳响应编码处理器接收到消息：$msg")
    val response = Response(actionId = msg.actionId, actionType = ActionTypeEnum.HeartBeat.id.toByte)
    out.add(response)
  }
}
