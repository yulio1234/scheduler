package com.zhongfei.scheduler.timer

import com.zhongfei.scheduler.utils.Logging

/**
 * 调度执行器，最原始的调度单元
 */
object ScheduleExecutor{
  def apply(id: String, delayMs: Long): ScheduleExecutor = new ScheduleExecutor(id, delayMs)
}
class ScheduleExecutor(val id:String,override val delayMs: Long) extends TimerTask with Logging {
  override def run(): Unit = {
    info("schedule executor id is "+id)
  }
}
