package com.zhongfei.scheduler.transport.codec

import com.zhongfei.scheduler.transport.protocol.SchedulerProtocol
import com.zhongfei.scheduler.transport.protocol.SchedulerProtocol.{Request, Response}
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToByteEncoder
import SchedulerProtocol.Protocol
import com.zhongfei.scheduler.utils.Logging

/**
 * 调度协议编码器
 */
class SchedulerProtocolEncoder extends MessageToByteEncoder[Protocol] with Logging {
  override def encode(ctx: ChannelHandlerContext, protocol: Protocol, out: ByteBuf): Unit = {
    out.writeByte(protocol.magic)
    out.writeByte(protocol.version)
    protocol match {
      case request: Request =>
        out.writeByte(request.protocolType)
        out.writeLong(request.actionId)
        out.writeByte(request.actionType)
        out.writeLong(request.timestamp)
        out.writeLong(request.expire)
        out.writeShort(request.length)
        if(request.content != null){
          out.writeBytes(request.content)
        }
      case response: Response =>
        out.writeByte(response.protocolType)
        out.writeLong(response.actionId)
        out.writeByte(response.actionType)
        out.writeBoolean(response.success)
        out.writeByte(response.errorCode)
        out.writeLong(response.timestamp)
        out.writeShort(response.length)
        if (response.content != null) {
          out.writeBytes(response.content)
        }
    }
  }
}
