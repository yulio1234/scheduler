package com.zhongfei.scheduler.timer

import org.scalatest.WordSpecLike

class TimingWheelStorageSpec extends WordSpecLike{
  "测试时间轮定时器" when{
    val timerStorage = TimingWheelStorage()
    timerStorage.save(ScheduleExecutor(1,0, null))
    timerStorage.save(ScheduleExecutor(2,0, null))
    timerStorage.save(ScheduleExecutor(3,0, null))
    timerStorage.save(ScheduleExecutor(4,0, null))
    timerStorage.save(ScheduleExecutor(5,0, null))
    timerStorage.save(ScheduleExecutor(6,0, null))
    timerStorage.save(ScheduleExecutor(7,0, null))
    Thread.sleep(1000)
    timerStorage.start()
    Thread.sleep(10000)
  }

}
