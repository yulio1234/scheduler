package com.zhongfei.scheduler.timer

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.persistence.typed.PersistenceId
import com.zhongfei.scheduler.timer.TimerEntity.{ScheduleAdd, ScheduleAddBody, ScheduleDel}
import com.zhongfei.scheduler.transfer.OperationResult
import org.scalatest.WordSpecLike

class TimerEntitySpec extends ScalaTestWithActorTestKit(
  s"""
      akka.persistence.journal.plugin = "cassandra-journal"
      akka.actor.serializers{
        jackson-cbor = "akka.serialization.jackson.JacksonCborSerializer"
      }
      akka.actor.serialization-bindings {
           "com.zhongfei.scheduler.timer.CborSerializable" = jackson-cbor
      }
      cassandra-journal {
         contact-points = ["192.168.20.198"]

      }
      cassandra-snapshot-store {
         contact-points = ["192.168.20.198"]
      }
    """) with WordSpecLike {
  "TimerEntity" must {
    "添加调度请求测试" in {
      val probe = createTestProbe[OperationResult]
      val actor = spawn(TimerEntity(PersistenceId("Timer", "1")))
      actor ! ScheduleAdd(1, ScheduleAddBody("test", "testEvent", "hello"), 2000, System.currentTimeMillis(), null, probe.ref)
      Thread.sleep(10000)
    }
    "删除调度请求测试" in{
      val probe = createTestProbe[OperationResult]
      val actor = spawn(TimerEntity(PersistenceId("Timer", "1")))
      actor ! ScheduleDel(1,null,probe.ref)
      Thread.sleep(10000)
    }
    "测试重启后数据重放" in{
      val probe = createTestProbe[OperationResult]
      val actor = spawn(TimerEntity(PersistenceId("Timer", "1")))
      actor ! ScheduleAdd(2, ScheduleAddBody("test", "testEvent", "hello"), 2000, System.currentTimeMillis(), null, probe.ref)
      Thread.sleep(10000)
    }
  }

}
