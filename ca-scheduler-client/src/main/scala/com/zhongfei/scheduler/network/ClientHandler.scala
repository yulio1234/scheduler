package com.zhongfei.scheduler.network

import akka.actor.typed.ActorRef
import com.zhongfei.scheduler.network.Dispatcher.{Command, ProtocolCommand}
import com.zhongfei.scheduler.transport.protocol.SchedulerProtocol.Protocol
import com.zhongfei.scheduler.utils.Logging
import io.netty.channel.{ChannelHandlerContext, SimpleChannelInboundHandler}

class ClientHandler(dispatcher:ActorRef[Command]) extends SimpleChannelInboundHandler[Protocol] with Logging{
  override def channelRead0(ctx: ChannelHandlerContext, msg: Protocol): Unit = {
    dispatcher ! ProtocolCommand(msg)
  }

  override def channelActive(ctx: ChannelHandlerContext): Unit = {
    info("连接服务器成功")
    super.channelActive(ctx)
  }

  override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable): Unit = {
    error("服务器链接失败",cause)
    ctx.close()
  }
}
