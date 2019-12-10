package com.zhongfei.scheduler.network

import java.net.InetSocketAddress

import com.zhongfei.scheduler.transport.Node
import com.zhongfei.scheduler.transport.codec.{SchedulerProtocolDecoder, SchedulerProtocolEncoder}
import com.zhongfei.scheduler.utils.Lifecycle
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.channel.{ChannelFuture, ChannelInitializer, ChannelOption}

class NettyServer(node:Node) extends Lifecycle[ChannelFuture,Unit]{
  private val bossGroup: NioEventLoopGroup = new NioEventLoopGroup(1)
  private val workerGroup:NioEventLoopGroup = new NioEventLoopGroup(Runtime.getRuntime.availableProcessors() * 2)
  private val bootstrap = new ServerBootstrap
  bootstrap.group(bossGroup, workerGroup)
    .channel(classOf[NioServerSocketChannel])
    .localAddress(new InetSocketAddress(node.host,node.port))
    .childHandler(new ChannelInitializer[SocketChannel]() {
      @throws[Exception]
      override protected def initChannel(socketChannel: SocketChannel): Unit = {
        socketChannel.pipeline
          .addLast(new SchedulerProtocolDecoder)
          .addLast(new SchedulerProtocolEncoder)
          .addLast(new RequestHandler)
          .addLast(new ResponseHandler)
      }
    }).childOption(ChannelOption.TCP_NODELAY, Boolean.box(true))
    .childOption(ChannelOption.SO_KEEPALIVE, Boolean.box(true))
  override def init(): ChannelFuture = {
      bootstrap.bind()
  }

  override def shutdown(): Unit = {
    bossGroup.shutdownGracefully()
    workerGroup.shutdownGracefully()
  }
}
