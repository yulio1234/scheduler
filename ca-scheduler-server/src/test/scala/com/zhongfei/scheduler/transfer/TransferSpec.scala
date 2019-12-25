package com.zhongfei.scheduler.transfer

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import com.zhongfei.scheduler.network.ApplicationManager.{HeartBeat, HeartBeaten}
import com.zhongfei.scheduler.options.ServerOption
import com.zhongfei.scheduler.transport.Node
import org.scalatest.WordSpecLike

class TransferSpec extends ScalaTestWithActorTestKit with WordSpecLike {
  "transfer测试" when{
    "operationResult" in{
      val actor = spawn(Transfer(ServerOption(Node("127.0.0.1", 2222)), null, null))
      actor ! HeartBeaten(123)
      Thread.sleep(10000)
    }
  }

}
