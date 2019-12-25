package com.zhongfei.scheduler.timer

import com.zhongfei.scheduler.timer.TimerEntity.ScheduleAddBody
import com.zhongfei.scheduler.utils.Logging

/**
 * 调度执行器，最原始的调度单元
 */
object ScheduleExecutor{
  def apply(id: Long, delayMs: Long,schedulerAddBody:ScheduleAddBody): ScheduleExecutor = new ScheduleExecutor(id, delayMs,schedulerAddBody)
}
class ScheduleExecutor(val id:Long,override val delayMs: Long,scheduleAddBody: ScheduleAddBody) extends TimerTask with Logging {
  override def run(): Unit = {
    debug(s"调度任务被触发，任务id=$id,body = $scheduleAddBody")
  }
}
