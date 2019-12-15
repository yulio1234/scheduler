package com.zhongfei.scheduler.transport.codec

import com.zhongfei.scheduler.transport.protocol.SchedulerProtocol
import com.zhongfei.scheduler.transport.protocol.SchedulerProtocol.{Request, Response}
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToByteEncoder
import SchedulerProtocol.Protocol

/**
 * 调度协议编码器
 */
class SchedulerProtocolEncoder extends MessageToByteEncoder[Protocol]{
  override def encode(ctx: ChannelHandlerContext, protocol: Protocol, out: ByteBuf): Unit = {
    protocol match {
      case request: Request =>
        out.writeByte(request.magic)
        out.writeByte(request.version)
        out.writeByte(request.protocolType)
        out.writeLong(request.actionId)
        out.writeByte(request.actionType)
        out.writeLong(request.timestamp)
        out.writeLong(request.expire)
        out.writeShort(request.length)
        out.writeBytes(request.content)
        out.writeInt(request.length)
        if(request.content != null){
          out.writeBytes(request.content)
        }
      case response :Response =>
        out.writeByte(response.magic)
        out.writeByte(response.version)
        out.writeByte(response.protocolType)
        out.writeLong(response.actionId)
        out.writeBoolean(response.success)
        out.writeByte(response.errorCode)
        out.writeLong(response.timestamp)
        out.writeInt(response.length)
        if(response.content != null){
          out.writeBytes(response.content)
        }
    }
  }
}
