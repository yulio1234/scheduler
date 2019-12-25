package com.zhongfei.scheduler.network

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import com.zhongfei.scheduler.network.codec.{RequestProtocolHandlerFactory, ResponseProtocolHandlerFactory}
import com.zhongfei.scheduler.options.ServerOption
import com.zhongfei.scheduler.transport.Node
import io.netty.channel.ChannelFuture
import org.scalatest.WordSpecLike

class NettyServerSpec  extends ScalaTestWithActorTestKit with WordSpecLike{
  "nettyServer测试" when {
    "发送网络请求" should {
      val node = Node("127.0.0.1", 2222)
      val actor = spawn(ServerDispatcher(ServerOption(node),null))
      val requestProtocolHandler = RequestProtocolHandlerFactory.create(actor)
      val responseProtocolHandler = ResponseProtocolHandlerFactory.create(actor)
      "发送心跳响应" in {
        val server = new NettyServer(node,requestProtocolHandler,responseProtocolHandler)
        server.init().addListener((future: ChannelFuture) => {
          info("链接结果："+future.isSuccess)
          info(""+future.channel())
        })
        Thread.sleep(1000000)
        info("测试结束")
      }
    }
  }
}
