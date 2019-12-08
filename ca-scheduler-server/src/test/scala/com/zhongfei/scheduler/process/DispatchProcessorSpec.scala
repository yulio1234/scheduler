package com.zhongfei.scheduler.process

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.testkit.TestKit
import com.zhongfei.scheduler.options.SingletonOption
import com.zhongfei.scheduler.process.DispatchProcessor.{Command, ProtocolCommand}
import com.zhongfei.scheduler.transport.protocol.SchedulerProtocol.{ActionTypeEnum, Request}
import com.zhongfei.scheduler.transport.{Node, Peer}
import org.scalatest.WordSpecLike
import scala.concurrent.duration._
import scala.concurrent.Await
class DispatchProcessorSpec extends ScalaTestWithActorTestKit with WordSpecLike  {
  "调度器" when{
    "接收到网络请求" should {
      "接收到心跳请求" in {

        val dispatcher = spawn(DispatchProcessor(SingletonOption(Node("127.0.0.1",2222))),"testDispatcher")

        val protocolCommand = ProtocolCommand(Peer("127.0.0.2", 2221, null), Request(actionId = 1, actionType = ActionTypeEnum.HeartBeat.id.toByte))
        dispatcher ! protocolCommand
        Thread.sleep(10000)
        println("sss")
      }
    }
  }


}

