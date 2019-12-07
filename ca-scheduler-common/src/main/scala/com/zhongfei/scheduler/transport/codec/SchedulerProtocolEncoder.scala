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
      case Request(magic, version, protocolType, actionId, actionType, timestamp, expire, length, content) =>
        out.writeByte(magic)
        out.writeByte(version)
        out.writeByte(protocolType)
        out.writeLong(actionId)
        out.writeByte(actionType)
        out.writeLong(timestamp)
        out.writeLong(expire)
        out.writeShort(length)
        out.writeBytes(content)
      case Response(magic, version, protocolType, actionId, success, errorCode, timestamp) =>
        out.writeByte(magic)
        out.writeByte(version)
        out.writeByte(protocolType)
        out.writeLong(actionId)
        out.writeBoolean(success)
        out.writeByte(errorCode)
        out.writeLong(timestamp)
    }
  }
}
