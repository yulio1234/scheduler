package com.zhongfei.scheduler.network

import java.net.InetSocketAddress

import akka.actor.typed.ActorRef
import com.zhongfei.scheduler.Message
import com.zhongfei.scheduler.network.CoreDispatcher.ProtocolCommand
import com.zhongfei.scheduler.transport.Peer
import com.zhongfei.scheduler.transport.protocol.SchedulerProtocol.Protocol
import com.zhongfei.scheduler.transport.protocol.{SchedulerProtocol => Protocol}
import com.zhongfei.scheduler.utils.Logging
import io.netty.channel.{ChannelHandlerContext, SimpleChannelInboundHandler}

/**
 * 服务端处理器
 * @param
 */
class ServerHandler(dispatcher: ActorRef[CoreDispatcher.Command]) extends SimpleChannelInboundHandler[Protocol] with Logging{


  override def channelActive(ctx: ChannelHandlerContext): Unit = {
    val peer = createPeer(ctx)
    info(s"链接已经活跃,$peer")
    super.channelActive(ctx)
  }

  /**
   * 连接断开
 *
   * @param ctx
   */
  override def channelInactive(ctx: ChannelHandlerContext): Unit = {
    val peer = createPeer(ctx)
    info(s"链接已经断开，$peer")
    super.channelInactive(ctx)
  }

  /**
   * 读取数据
   * @param ctx
   */
  override def channelRead0(ctx: ChannelHandlerContext, protocol: Protocol.Protocol): Unit = {
    val peer:Peer = createPeer(ctx)
    info(s"读取到客户端数据，$Peer")
    //将netty消息发送全局处理器，如果遇到问题，应该由死信队列处理
    dispatcher ! ProtocolCommand(peer,protocol)
  }

  /**
   * 捕获异常
   * @param ctx
   * @param cause
   */
  override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable): Unit = {
    val peer = createPeer(ctx)
    error(s"服务器发生异常，$peer",cause)
    // TODO: 直接关闭会将缓存中的数据清除掉，处理不好会造成数据丢失，可以参考bookkeeper框架的做法，检测到链接关闭请求，在其他地方进行处理
    //        if (cause instanceof ClosedChannelException) {
    //            LOG.info("Client died before request could be completed", cause);
    //            return;
    //        }
    ctx.close()
  }

  def createPeer(ctx: ChannelHandlerContext): Peer ={
    val iSocketAddress = ctx.channel.localAddress.asInstanceOf[InetSocketAddress]
    val host = iSocketAddress.getAddress.getHostAddress
    val port = iSocketAddress.getPort
    Peer(host,port,ctx.channel())
  }
}
