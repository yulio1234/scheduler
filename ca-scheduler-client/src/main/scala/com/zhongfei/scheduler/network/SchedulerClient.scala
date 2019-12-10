package com.zhongfei.scheduler.network

import java.net.InetSocketAddress

import akka.actor.typed.ActorRef
import com.zhongfei.scheduler.network.Dispatcher.Command
import com.zhongfei.scheduler.transport.Peer
import com.zhongfei.scheduler.transport.codec.{SchedulerProtocolDecoder, SchedulerProtocolEncoder}
import com.zhongfei.scheduler.utils.{Lifecycle, Logging}
import io.netty.bootstrap.Bootstrap
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.channel.{ChannelFuture, ChannelInitializer}
import io.netty.util.concurrent.{Future, Promise}

class SchedulerClient(host: String, port: Int, dispatcher: ActorRef[Command]) extends Lifecycle[Future[Peer],Unit ] with Logging {
  private val nioEventLoopGroup: NioEventLoopGroup = new NioEventLoopGroup()
  private val bootstrap = new Bootstrap
//  bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, Integer)
//  bootstrap.option(ChannelOption.TCP_NODELAY, Boolean.box(true))
//  bootstrap.option(ChannelOption.SO_KEEPALIVE, Boolean.box(true))
  bootstrap.group(nioEventLoopGroup)
  bootstrap.channel(classOf[NioSocketChannel])
  bootstrap.remoteAddress(new InetSocketAddress(host,port))
  bootstrap.handler(new ChannelInitializer[SocketChannel]() {
    @throws[Exception]
    override protected def initChannel(socketChannel: SocketChannel): Unit = {
      val pipeline = socketChannel.pipeline
      pipeline.addLast(new SchedulerProtocolDecoder())
      pipeline.addLast(new SchedulerProtocolEncoder())
      pipeline.addLast(new ClientHandler(dispatcher))
    }
  })

  override def init(): Future[Peer] = {
    val promise:Promise[Peer] = bootstrap.config().group().next().newPromise()
    info(s"创建客户端,host=${host},url=${port}")
    bootstrap.connect(host, port).addListener((future: ChannelFuture) => {
      if (future.isSuccess) {
        //如果链接成功，就发送链接给分发器
        promise.setSuccess(Peer(host, port, future.channel()))
      }else{
        promise.setFailure(future.cause())
      }
    })
    promise
  }

  override def shutdown():Unit   = {
    nioEventLoopGroup.shutdownGracefully()
  }
}