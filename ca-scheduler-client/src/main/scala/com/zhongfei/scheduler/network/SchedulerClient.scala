package com.zhongfei.scheduler.network

import java.net.InetSocketAddress

import com.zhongfei.scheduler.network.codec.{RequestProtocolHandler, ResponseProtocolHandler}
import com.zhongfei.scheduler.transport.Node
import com.zhongfei.scheduler.transport.codec.{SchedulerProtocolDecoder, SchedulerProtocolEncoder}
import com.zhongfei.scheduler.utils.Logging
import io.netty.bootstrap.Bootstrap
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.channel.{ChannelFuture, ChannelInitializer, ChannelOption}
import io.netty.handler.codec.{LengthFieldBasedFrameDecoder, LengthFieldPrepender}

class SchedulerClient(requestProtocolHandler: RequestProtocolHandler,responseProtocolHandler: ResponseProtocolHandler) extends Logging {
  private val nioEventLoopGroup: NioEventLoopGroup = new NioEventLoopGroup()
  private val bootstrap = new Bootstrap
  bootstrap.option(ChannelOption.TCP_NODELAY, Boolean.box(true))
  bootstrap.option(ChannelOption.SO_KEEPALIVE, Boolean.box(true))
  bootstrap.group(nioEventLoopGroup)
  bootstrap.channel(classOf[NioSocketChannel])
  bootstrap.handler(new ChannelInitializer[SocketChannel]() {
    @throws[Exception]
    override protected def initChannel(socketChannel: SocketChannel): Unit = {
      val pipeline = socketChannel.pipeline
      pipeline.addLast(new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 4, 0, 4))
      pipeline.addLast(new LengthFieldPrepender(4))
      pipeline.addLast(new SchedulerProtocolDecoder())
      pipeline.addLast(new SchedulerProtocolEncoder())
//      pipeline.addLast(new RequestHandler(requestProtocolHandler))
//      pipeline.addLast(new ResponseHandler(responseProtocolHandler))
    }
  })

  def createConnection(node:Node): ChannelFuture ={
    info(s"创建客户端,host=${node.host},url=${node.port}")
    bootstrap.remoteAddress(new InetSocketAddress(node.host,node.port))
    bootstrap.connect()
  }

  def shutdown():Unit   = {
    nioEventLoopGroup.shutdownGracefully()
  }
}
