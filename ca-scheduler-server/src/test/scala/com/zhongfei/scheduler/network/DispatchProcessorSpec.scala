package com.zhongfei.scheduler.network

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import com.zhongfei.scheduler.options.SingletonOption
import com.zhongfei.scheduler.transport.protocol.SchedulerProtocol.{ActionTypeEnum, Request}
import com.zhongfei.scheduler.transport.{Node, Peer}
import com.zhongfei.scheduler.utils.Logging
import io.netty.channel.ChannelFuture
import org.scalatest.WordSpecLike

import scala.concurrent.Await
class DispatchProcessorSpec extends ScalaTestWithActorTestKit with WordSpecLike with Logging {
  "调度器" when{
    "接收到网络请求" should {
        val node = Node("127.0.0.1", 2222)
        val dispatcher = spawn(ServerDispatcher(SingletonOption(node)),"testDispatcher")
      "接收到心跳请求" in {


      }
      "测试netty服务端" in{
        val server = new NettyServer(node)
        server.init().addListener((future:ChannelFuture)=>{
          println(future.isSuccess)
        })
        info("结束")
        Await.wait(100000)
      }
    }
  }


}

