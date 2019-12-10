package com.zhongfei.scheduler.network

import com.zhongfei.scheduler.network.codec.RequestProtocolHandler
import com.zhongfei.scheduler.transport.Peer
import com.zhongfei.scheduler.transport.protocol.SchedulerProtocol.Request
import com.zhongfei.scheduler.utils.{Logging, RemotingUtil}
import io.netty.channel.{Channel, ChannelHandlerContext, SimpleChannelInboundHandler}

/**
 * 服务端处理器
 * @param
 */
class RequestHandler(handler:RequestProtocolHandler) extends SimpleChannelInboundHandler[Request] with Logging{


  override def channelActive(ctx: ChannelHandlerContext): Unit = {
    val peer = createPeer(ctx.channel())
    info(s"链接已经活跃,$peer")
    super.channelActive(ctx)
  }

  /**
   * 连接断开
 *
   * @param ctx
   */
  override def channelInactive(ctx: ChannelHandlerContext): Unit = {
    val peer = createPeer(ctx.channel())
    info(s"链接已经断开，$peer")
    super.channelInactive(ctx)
  }


  override def channelRead0(ctx: ChannelHandlerContext, msg: Request): Unit = {
    info(s"读取到消息$msg")
    //处理请求协议
    handler.handle(msg,ctx.channel())
  }


  /**
   * 捕获异常
   * @param ctx
   * @param cause
   */
  override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable): Unit = {
    val peer = createPeer(ctx.channel())
    error(s"服务器发生异常，$peer",cause)
    // TODO: 直接关闭会将缓存中的数据清除掉，处理不好会造成数据丢失，可以参考bookkeeper框架的做法，检测到链接关闭请求，在其他地方进行处理
    //        if (cause instanceof ClosedChannelException) {
    //            LOG.info("Client died before request could be completed", cause);
    //            return;
    //        }
    ctx.close()
  }

  def createPeer(channel:Channel): Peer ={
    val ip = RemotingUtil.parseRemoteIP(channel)
    val port = RemotingUtil.parseRemotePort(channel)
    Peer(ip,port,channel)
  }
}
