package com.zhongfei.scheduler.timer
import com.zhongfei.scheduler.utils.{Logging, ShutdownableThread}

/**
 * 基于层级时间轮的调度仓库
 */
object TimingWheelStorage{
  def apply(reaperEnabled: Boolean = true,clockInterval:Long = 200L): TimingWheelStorage = new TimingWheelStorage()
}

class TimingWheelStorage(reaperEnabled: Boolean = true,clockInterval:Long = 200L) extends Operation[ScheduleExecutor] with Logging{
  private val expirationReaper = new ExpiredOperationReaper()
  private[this] val timer:Timer = new SystemTimer("timingWheelTimer")
  private[this] var executors = Map.empty[Long,ScheduleExecutor]
  override def save(executor: ScheduleExecutor): Unit = {
    executors += executor.id -> executor
    timer.add(executor)
  }


  /**
   * 如果默认启动
   */
  if(reaperEnabled)
    expirationReaper.start()

  override def find(id: Long): ScheduleExecutor = {
    executors(id)
  }

  override def delete(id: Long): Unit = {
    executors.get(id) match {
      case Some(v) => {
        v.cancel()
        executors -= id
      }
      case None => throw new NullPointerException(s"can not delete,this id is not fund:  $id")
    }
  }
  def shutdown():Unit = {
    timer.shutdown()
  }
  private class ExpiredOperationReaper extends ShutdownableThread("schedule storage reaper",false){
    /**
     * This method is repeatedly invoked until the thread shuts down or this method throws an exception
     */
    override def doWork(): Unit = {
      timer.advanceClock(clockInterval);
    }
  }
}
