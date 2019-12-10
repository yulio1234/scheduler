package com.zhongfei.scheduler.network

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import com.zhongfei.scheduler.utils.Logging
import org.scalatest.WordSpecLike
import com.zhongfei.scheduler.network.Command.SchedulerCommand
import com.zhongfei.scheduler.transport.Node
import io.netty.channel.ChannelFuture
class SchedulerClientTSpec extends ScalaTestWithActorTestKit with WordSpecLike with Logging  {
  "测试netty客户端" when{
    "发起连接" should{
      "发送心跳请求" in{
        val actor = createTestProbe[Dispatcher.Command].ref
        val client = new SchedulerClient(Node("127.0.0.1", 2222),actor)
        val peer = client.init().get()

        peer.channel.writeAndFlush()
      }
    }
  }

}
