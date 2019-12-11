package com.zhongfei.scheduler.network

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import com.zhongfei.scheduler.transport.Node
import com.zhongfei.scheduler.transport.protocol.SchedulerProtocol.{ActionTypeEnum, Request}
import com.zhongfei.scheduler.utils.Logging
import io.netty.channel.ChannelFuture
import org.scalatest.WordSpecLike
class SchedulerClientTSpec extends ScalaTestWithActorTestKit with WordSpecLike  {
  "测试netty客户端" when{
    "发起连接" should{
      val actor = createTestProbe[Dispatcher.Command].ref
      val client = new SchedulerClient(Node("127.0.0.1", 2222), actor)
      val peer = client.init().get()
      val bytes = "test-client".getBytes
      "发送心跳请求" in{
        val request = Request(actionId = 1, actionType = ActionTypeEnum.HeartBeat.id.toByte, length = bytes.length.toShort, content = bytes)
        peer.channel.writeAndFlush(request).sync().addListener{(future:ChannelFuture)=>
          info(future.isSuccess.toString)

        }
        peer.channel.close().sync()
      }
      "发起取消注册请求" in {
        val request = Request(actionId = 1, actionType = ActionTypeEnum.Unregister.id.toByte, length = bytes.length.toShort, content = bytes)
        peer.channel.writeAndFlush(request).sync().addListener{(future:ChannelFuture)=>
          info(future.isSuccess.toString)

        }
        peer.channel.close().sync()
      }
    }
  }

}
