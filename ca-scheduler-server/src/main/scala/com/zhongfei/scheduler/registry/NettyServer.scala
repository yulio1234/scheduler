package com.zhongfei.scheduler.registry

import akka.actor.typed.ActorRef
import com.zhongfei.scheduler.Message
import com.zhongfei.scheduler.transport.Node
import com.zhongfei.scheduler.transport.codec.{SchedulerProtocolDecoder, SchedulerProtocolEncoder}
import com.zhongfei.scheduler.utils.Lifecycle
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.channel.{ChannelInitializer, ChannelOption}

class NettyServer(node: Node,processor: ActorRef[Message]) extends Lifecycle{
  private var bossGroup:NioEventLoopGroup = null
  private var workerGroup:NioEventLoopGroup = null

  override def init(): Unit = {
    bossGroup = new NioEventLoopGroup(1)
    workerGroup = new NioEventLoopGroup(Runtime.getRuntime.availableProcessors() * 2)
    val bootstrap = new ServerBootstrap
    bootstrap.group(bossGroup, workerGroup)
      .channel(classOf[NioServerSocketChannel])
      .childHandler(new ChannelInitializer[SocketChannel]() {
      @throws[Exception]
      override protected def initChannel(socketChannel: SocketChannel): Unit = {
        socketChannel.pipeline
          .addLast(new SchedulerProtocolDecoder)
          .addLast(new SchedulerProtocolEncoder)
          .addLast(new ServerHandler(processor))
      }
    }).childOption(ChannelOption.TCP_NODELAY, Boolean.box(true))
      .childOption(ChannelOption.SO_KEEPALIVE, Boolean.box(true))
      .bind(node.host, node.port)
      .syncUninterruptibly
  }

  override def shutdown(): Unit = {
    if (bossGroup != null) {
      bossGroup.shutdownGracefully()
      bossGroup = null
    }

    if (workerGroup != null) {
      workerGroup.shutdownGracefully()
      workerGroup = null
    }
  }
}
