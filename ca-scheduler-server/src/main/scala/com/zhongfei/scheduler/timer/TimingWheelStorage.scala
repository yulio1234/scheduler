package com.zhongfei.scheduler.timer
import com.zhongfei.scheduler.utils.{Logging, ShutdownableThread}

/**
 * 基于层级时间轮的调度仓库
 */
object TimingWheelStorage extends Logging {
  def apply(reaperEnabled: Boolean = false,clockInterval:Long = 200L): TimingWheelStorage = new TimingWheelStorage(reaperEnabled,clockInterval)
}

class TimingWheelStorage(reaperEnabled: Boolean,clockInterval:Long) extends Operation[ScheduleExecutor] with Logging{
  debug("实例化层级时间轮定时器:"+this)
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
  if(reaperEnabled) {
    debug("初始化状态："+reaperEnabled)
    start
  }


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
  def start():Unit = {
    debug("启动时间轮")
    expirationReaper.start()
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
