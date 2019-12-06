package com.zhongfei.scheduler.transport.codec

import java.util

import com.zhongfei.scheduler.transport.protocol.SchedulerProtocol.{Request, Response}
import com.zhongfei.scheduler.transport.protocol.{SchedulerProtocol => protocl}
import com.zhongfei.scheduler.transport.{SchedulerExceptions => exceptions}
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.ByteToMessageDecoder
/**
 * 调度协议解码器
 */
class SchedulerProtocolDecoder extends ByteToMessageDecoder{
  override def decode(ctx: ChannelHandlerContext, in: ByteBuf, out: util.List[AnyRef]): Unit = {
    val magic = in.readByte()
    //检测协议魔数是否正常
    if(magic != protocl.magic){
      throw exceptions.ProtocolMagicException()
    }else{
      //如果版本不对，就抛出协议版本异常
      val version = in.readByte()
      if(version != protocl.version){
        throw exceptions.ProtocolVersionException()
      }else{
        //协议类型
        val protocolType = in.readByte()
        //处理请求协议
        if (protocolType == protocl.ProtocolTypeEnum.Request) {
          val actionId = in.readLong()
          val actionType = in.readByte()
          val timestamp = in.readLong()
          val expire = in.readLong()
          val length = in.readShort()
          val bytes = new Array[Byte](length)
          in.readBytes(bytes)
          val request = Request(magic,version,protocolType,actionId,actionType,timestamp,expire,length,bytes)
          out.add(request)
          //处理响应协议
        }else if(protocolType == protocl.ProtocolTypeEnum.Response){
          val actionId = in.readLong()
          val success = in.readBoolean()
          val errorByte = in.readByte()
          val timestamp = in.readLong()
          val response = Response(magic,version,protocolType,actionId,success,errorByte,timestamp)
          out.add(response)
        }
      }
    }
  }
}
