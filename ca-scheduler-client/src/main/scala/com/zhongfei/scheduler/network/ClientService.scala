package com.zhongfei.scheduler.network

import akka.actor.typed.ActorRef
import com.zhongfei.scheduler.network.Dispatcher.{Command, ConnectionCompleted}
import com.zhongfei.scheduler.transport.Peer
import com.zhongfei.scheduler.transport.codec.{SchedulerProtocolDecoder, SchedulerProtocolEncoder}
import com.zhongfei.scheduler.utils.{Lifecycle, Logging}
import io.netty.bootstrap.Bootstrap
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.channel.{ChannelFuture, ChannelInitializer, ChannelOption}

class ClientService(host:String, port:Int,dispatcher:ActorRef[Command]) extends Lifecycle with Logging{
  private var nioEventLoopGroup:NioEventLoopGroup = null

override def init(): Unit = {
    nioEventLoopGroup = new NioEventLoopGroup()
    val bootstrap = new Bootstrap
    bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 2000)
    bootstrap.option(ChannelOption.TCP_NODELAY, true)
    bootstrap.option(ChannelOption.SO_KEEPALIVE, true)
    bootstrap.group(nioEventLoopGroup)
    bootstrap.channel(classOf[NioSocketChannel])
    bootstrap.handler(new ChannelInitializer[SocketChannel]() {
      @throws[Exception]
      override protected def initChannel(socketChannel: SocketChannel): Unit = {
        val pipeline = socketChannel.pipeline
        pipeline.addLast(new SchedulerProtocolDecoder())
        pipeline.addLast(new SchedulerProtocolEncoder())
        pipeline.addLast(new ClientHandler(dispatcher))
      }
    })

    info(s"创建客户端,host=${host},url=${port}")
    bootstrap.connect(host,port).addListener((future: ChannelFuture)=>{
      if (future.isSuccess) {
        //如果链接成功，就发送链接给分发器
        dispatcher ! ConnectionCompleted(Peer(host,port,future.channel()))
      }
    })

  }

  override def shutdown(): Unit = {

  }
}
