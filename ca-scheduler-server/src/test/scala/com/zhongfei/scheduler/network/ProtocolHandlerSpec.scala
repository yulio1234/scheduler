package com.zhongfei.scheduler.network

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import com.zhongfei.scheduler.command.SchedulerCommand.HeartBeat
import com.zhongfei.scheduler.network.codec.RequestProtocolHandlerFactory
import com.zhongfei.scheduler.transport.protocol.SchedulerProtocol.{ActionTypeEnum, Request}
import com.zhongfei.scheduler.utils.Logging
import org.scalatest.WordSpecLike

class ProtocolHandlerSpec extends ScalaTestWithActorTestKit with WordSpecLike with Logging {
  "网络通讯协议处理器" when {
    "请求处理器" should {
      val actor = createTestProbe[ServerDispatcher.Command]()
      val requestProtocolHandler = RequestProtocolHandlerFactory.create(actor.ref)
      val array = "test-application".getBytes
      "处理心跳请求"in {
        val request = Request(actionId = 1, actionType = ActionTypeEnum.HeartBeat.id.toByte,length = array.length.toShort,context = array)
        requestProtocolHandler.handle(request,null)
        actor.expectMessageType[HeartBeat]
      }
      "处理取消注册请求" in {

      }
    }
  }

}
