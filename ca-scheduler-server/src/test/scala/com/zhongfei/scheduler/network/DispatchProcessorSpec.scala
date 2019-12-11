package com.zhongfei.scheduler.network

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import com.zhongfei.scheduler.network.codec.RequestProtocolHandlerFactory
import com.zhongfei.scheduler.options.SingletonOption
import com.zhongfei.scheduler.transport.protocol.SchedulerProtocol.{ActionTypeEnum, Request}
import com.zhongfei.scheduler.transport.{Node, Peer}
import com.zhongfei.scheduler.utils.Logging
import io.netty.channel.ChannelFuture
import org.scalatest.WordSpecLike

import scala.concurrent.Await

class DispatchProcessorSpec extends ScalaTestWithActorTestKit with WordSpecLike {
  "调度器" when {
    "接收到网络请求" should {
      val actor = createTestProbe[ServerDispatcher.Command]()
      val node = Node("127.0.0.1", 2222)
      val requestProtocolHandler = RequestProtocolHandlerFactory.create(actor.ref)
      "接收到心跳请求" in {


      }
      "测试netty服务端" in {
        val server = new NettyServer(node,requestProtocolHandler)
        server.init().addListener((future: ChannelFuture) => {
          info("链接结果："+future.isSuccess)
        })
        Thread.sleep(1000000)
        info("测试结束")
      }
    }
  }


}

