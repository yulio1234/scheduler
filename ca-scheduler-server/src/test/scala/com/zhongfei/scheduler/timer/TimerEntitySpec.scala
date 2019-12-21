package com.zhongfei.scheduler.timer

import java.util.UUID

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.persistence.typed.PersistenceId
import com.zhongfei.scheduler.timer.TimerEntity.{ScheduleAdd, ScheduleAddBody}
import org.scalatest.WordSpecLike

class TimerEntitySpec extends ScalaTestWithActorTestKit(s"""
      akka.persistence.journal.plugin = "akka.persistence.journal.inmem"
      akka.persistence.snapshot-store.plugin = "akka.persistence.snapshot-store.local"
      akka.persistence.snapshot-store.local.dir = "target/snapshot-${UUID.randomUUID().toString}"
    """) with WordSpecLike {
  "TimerEntity" must{
      "Init" in{
        val probe = createTestProbe[TimerEntity.OperationResult]
        val actor = spawn(TimerEntity(PersistenceId("Timer", "1")))
        actor ! ScheduleAdd(1,ScheduleAddBody("test","testEvent","hello"),2000,System.currentTimeMillis(),probe.ref)
        probe.expectMessage(TimerEntity.Succeeded)
        
      }
  }

}
