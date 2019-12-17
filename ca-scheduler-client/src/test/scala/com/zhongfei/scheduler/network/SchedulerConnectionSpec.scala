package com.zhongfei.scheduler.network

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import com.zhongfei.scheduler.network.Dispatcher.HeartBeat
import com.zhongfei.scheduler.network.SchedulerConnection.Initialize
import com.zhongfei.scheduler.network.SchedulerConnectionManager.{Connected, ServerTerminate, Unreachable}
import com.zhongfei.scheduler.network.codec.{RequestProtocolHandlerFactory, ResponseProtocolHandlerFactory}
import com.zhongfei.scheduler.transport.Node
import org.scalatest.WordSpecLike

class SchedulerConnectionSpec extends ScalaTestWithActorTestKit with WordSpecLike {
  "scheduler client test" when {
    "初始化client" should {
      val actor1 = createTestProbe[Dispatcher.Message]
      val actor2 = createTestProbe[SchedulerConnectionManager.Message]
      val requestProtocolHandler = RequestProtocolHandlerFactory.create(actor1.ref)
      val responseProtocolHandler = ResponseProtocolHandlerFactory.create(actor1.ref)
      val client = new SchedulerClient(requestProtocolHandler,responseProtocolHandler)
      val connection = spawn(SchedulerConnection(ClientOption("test"), Node("127.0.0.1", 2222), actor2.ref,actor1.ref,client))
      "进行初始化 没有服务端" in {
        connection ! Initialize
        actor2.expectMessageType[Unreachable]
      }
      "进行初始化 有服务端" in{
        connection ! Initialize
        actor2.expectMessageType[Connected]
        actor1.expectMessageType[HeartBeat]
        connection ! ServerTerminate
      }
    }
    "测试dispatch" should{

      val actor1 = spawn(Dispatcher(ClientOption("test")))
      val actor2 = createTestProbe[SchedulerConnectionManager.Message]
      val requestProtocolHandler = RequestProtocolHandlerFactory.create(actor1)
      val responseProtocolHandler = ResponseProtocolHandlerFactory.create(actor1)
      val client = new SchedulerClient(requestProtocolHandler,responseProtocolHandler)
      val connection = spawn(SchedulerConnection(ClientOption("test"), Node("127.0.0.1", 2222), actor2.ref,actor1,client))
      "进行初始化 有服务端" in{
        connection ! Initialize
//        actor2.expectMessageType[Connected]
        Thread.sleep(30000)
        connection ! ServerTerminate
      }
    }
  }
}
