package com.zhongfei.scheduler.network

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import com.zhongfei.scheduler.network.codec.{RequestProtocolHandlerFactory, ResponseProtocolHandlerFactory}
import com.zhongfei.scheduler.transport.Node
import com.zhongfei.scheduler.transport.protocol.SchedulerProtocol.{ActionTypeEnum, Request}
import io.netty.channel.ChannelFuture
import org.scalatest.WordSpecLike
class SchedulerClientTSpec extends ScalaTestWithActorTestKit with WordSpecLike  {
  "测试netty客户端" when{
    "发起连接" should{
      val actor = createTestProbe[SchedulerConnectionManager.Message].ref
      val requestProtocolHandler = RequestProtocolHandlerFactory.create(actor.ref)
      val responseProtocolHandler = ResponseProtocolHandlerFactory.create(actor.ref)
      val client = new SchedulerClient(requestProtocolHandler,responseProtocolHandler)
      val channel = client.createConnection(Node("127.0.0.1", 2222)).channel()
      val bytes = "test-client".getBytes
      "发送心跳请求" in{
        val request = Request(actionId = 1, actionType = ActionTypeEnum.HeartBeat.id.toByte, length = bytes.length.toShort, content = bytes)
        channel.writeAndFlush(request).sync().addListener{(future:ChannelFuture)=>
          info(future.isSuccess.toString)

        }
        channel.close().sync()
      }
      "发起取消注册请求" in {
        val request = Request(actionId = 1, actionType = ActionTypeEnum.Unregister.id.toByte, length = bytes.length.toShort, content = bytes)
        channel.writeAndFlush(request).sync().addListener{(future:ChannelFuture)=>
          info(future.isSuccess.toString)

        }
        channel.close().sync()
      }
    }
  }

}
