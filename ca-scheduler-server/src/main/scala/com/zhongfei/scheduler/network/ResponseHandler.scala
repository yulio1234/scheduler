package com.zhongfei.scheduler.network

import com.zhongfei.scheduler.transport.Peer
import com.zhongfei.scheduler.transport.protocol.SchedulerProtocol.Response
import com.zhongfei.scheduler.utils.Logging
import io.netty.channel.{ChannelHandlerContext, SimpleChannelInboundHandler}
import com.zhongfei.scheduler.network.codec.ResponseProtocolHandler
class ResponseHandler(responseProtocolHandler: ResponseProtocolHandler)  extends SimpleChannelInboundHandler[Response] with Logging{

  override def channelRead0(ctx: ChannelHandlerContext, msg: Response): Unit = {
    debug(s"读取到客户端数据，$Peer")

  }

  override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable): Unit = {
    error(s"服务器发生异常",cause)
    ctx.close()
  }

}
