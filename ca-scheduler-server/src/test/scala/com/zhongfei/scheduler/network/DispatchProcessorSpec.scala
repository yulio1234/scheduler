package com.zhongfei.scheduler.network

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import com.zhongfei.scheduler.network.codec.{RequestProtocolHandlerFactory, ResponseProtocolHandlerFactory}
import com.zhongfei.scheduler.options.SingletonOption
import com.zhongfei.scheduler.transport.Node
import io.netty.channel.ChannelFuture
import org.scalatest.WordSpecLike

class DispatchProcessorSpec extends ScalaTestWithActorTestKit with WordSpecLike {
  "调度器" when {
    "接收到网络请求" should {
      val node = Node("192.168.1.2", 2222)
      val actor = spawn(ServerDispatcher(SingletonOption(node)))
      val requestProtocolHandler = RequestProtocolHandlerFactory.create(actor)
      val responseProtocolHandler = ResponseProtocolHandlerFactory.create(actor)
      "接收到心跳请求" in {

      }
      "测试netty服务端" in {
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

