package com.zhongfei.scheduler.network.codec

import java.util

import com.zhongfei.scheduler.network.Dispatcher.HeartBeat
import com.zhongfei.scheduler.transport.protocol.SchedulerProtocol.{ActionTypeEnum, Request}
import com.zhongfei.scheduler.utils.Logging
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToMessageEncoder

/**
 * 心跳消息编码器
 */
class HearBeatToRequestEncoder extends MessageToMessageEncoder[HeartBeat] with Logging{
  override def encode(ctx: ChannelHandlerContext, msg: HeartBeat, out: util.List[AnyRef]): Unit = {
    debug(s"心跳编码器收到心跳发送请求，转换心跳消息：$msg")
    val bytes = msg.appName.getBytes()
    val request = Request(actionId = msg.actionId, actionType = ActionTypeEnum.HeartBeat.id.toByte, length = bytes.length.toShort, content = bytes)
    out.add(request)
  }
}
