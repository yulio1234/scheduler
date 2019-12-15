package com.zhongfei.scheduler.network

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import com.zhongfei.scheduler.network.SchedulerConnection.Initialize
import com.zhongfei.scheduler.network.SchedulerConnectionManager.{Connected, Unreachable}
import com.zhongfei.scheduler.network.codec.{RequestProtocolHandlerFactory, ResponseProtocolHandlerFactory}
import com.zhongfei.scheduler.transport.Node
import org.scalatest.WordSpecLike

class SchedulerConnectionSpec extends ScalaTestWithActorTestKit with WordSpecLike {
  "scheduler client test" when {
    "初始化client" should {
      val actor = createTestProbe[SchedulerConnectionManager.Message]
      val requestProtocolHandler = RequestProtocolHandlerFactory.create(actor.ref)
      val responseProtocolHandler = ResponseProtocolHandlerFactory.create(actor.ref)
      val client = new SchedulerClient(requestProtocolHandler,responseProtocolHandler)
      val connection = spawn(SchedulerConnection(ClientOption("test"), Node("127.0.0.1", 2222), actor.ref,client))
      "进行初始化 没有服务端" in {
        connection ! Initialize
        actor.expectMessageType[Unreachable]
      }
      "进行初始化 有服务端" in{
        connection ! Initialize
        actor.expectMessageType[Connected]
        connection !
      }
    }
  }
}
