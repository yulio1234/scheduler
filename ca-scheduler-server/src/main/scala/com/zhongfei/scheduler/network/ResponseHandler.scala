package com.zhongfei.scheduler.network

import java.net.InetSocketAddress

import com.zhongfei.scheduler.transport.Peer
import com.zhongfei.scheduler.transport.protocol.SchedulerProtocol.{Request, Response}
import com.zhongfei.scheduler.utils.{Logging, RemotingUtil}
import io.netty.channel.{Channel, ChannelHandlerContext, SimpleChannelInboundHandler}

class ResponseHandler  extends SimpleChannelInboundHandler[Response] with Logging{

  override def channelRead0(ctx: ChannelHandlerContext, msg: Response): Unit = {
    info(s"读取到客户端数据，$Peer")
  }

  override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable): Unit = {
    error(s"服务器发生异常",cause)
    ctx.close()
  }

}
