package com.zhongfei.scheduler.timer

import com.zhongfei.scheduler.utils.{Logging, ShutdownableThread}
import org.junit.Test

class ThreadTest extends ShutdownableThread("test",false) with Logging{
  /**
   * This method is repeatedly invoked until the thread shuts down or this method throws an exception
   */
  override def doWork(): Unit = {
    info("thread test")
  }
  @Test
  def test(): Unit ={
    new ThreadTest

  }
}
