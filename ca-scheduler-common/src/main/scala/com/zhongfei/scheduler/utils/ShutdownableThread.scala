package com.zhongfei.scheduler.utils

import java.util.concurrent.{CountDownLatch, TimeUnit}


abstract class ShutdownableThread(val name: String, val isInterruptible: Boolean = true)
        extends Thread(name) with Logging {
  this.setDaemon(false)
  this.logIdent = "[" + name + "]: "
  private val shutdownInitiated = new CountDownLatch(1)
  private val shutdownComplete = new CountDownLatch(1)
  @volatile private var isStarted: Boolean = false
  
  def shutdown(): Unit = {
    initiateShutdown()
    awaitShutdown()
  }

  def isShutdownInitiated: Boolean = shutdownInitiated.getCount == 0

  def isShutdownComplete: Boolean = shutdownComplete.getCount == 0

  /**
    * @return true if there has been an unexpected error and the thread shut down
    */
  // mind that run() might set both when we're shutting down the broker
  // but the return value of this function at that point wouldn't matter
  def isThreadFailed: Boolean = isShutdownComplete && !isShutdownInitiated

  def initiateShutdown(): Boolean = {
    this.synchronized {
      if (isRunning) {
        info("Shutting down")
        shutdownInitiated.countDown()
        if (isInterruptible)
          interrupt()
        true
      } else
        false
    }
  }

  /**
   * After calling initiateShutdown(), use this API to wait until the shutdown is complete
   */
  def awaitShutdown(): Unit = {
    if (!isShutdownInitiated)
      throw new IllegalStateException("initiateShutdown() was not called before awaitShutdown()")
    else {
      if (isStarted)
        shutdownComplete.await()
      info("Shutdown completed")
    }
  }

  /**
   *  Causes the current thread to wait until the shutdown is initiated,
   *  or the specified waiting time elapses.
   *
   * @param timeout
   * @param unit
   */
  def pause(timeout: Long, unit: TimeUnit): Unit = {
    if (shutdownInitiated.await(timeout, unit))
      trace("shutdownInitiated latch count reached zero. Shutdown called.")
  }

  /**
   * This method is repeatedly invoked until the thread shuts down or this method throws an exception
   */
  def doWork(): Unit

  override def run(): Unit = {
    isStarted = true
    info("Starting")
    try {
      while (isRunning)
        doWork()
    } catch {
      case e: Throwable =>
        if (isRunning)
          error("Error due to", e)
    } finally {
       shutdownComplete.countDown()
    }
    info("Stopped")
  }
  def isRunning: Boolean = !isShutdownInitiated
}
