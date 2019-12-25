package com.zhongfei.scheduler.network

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import com.zhongfei.scheduler.options.ServerOption
import com.zhongfei.scheduler.transport.Node
import org.scalatest.WordSpecLike

class ApplicationGroupSpec extends ScalaTestWithActorTestKit with WordSpecLike{
  "应用组启动时"when{
    "测试应用关闭" in{
      val node = Node("127.0.0.1", 2222)
      val option = ServerOption(node)
      val actor = spawn(ApplicationGroup(option,"test-group"))
      actor
    }
  }
}
