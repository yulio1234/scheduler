package com.zhongfei.scheduler.utils

import java.lang.Thread.UncaughtExceptionHandler

/**
 * thread创建器
 */
object SchedulerThread{
  def apply(runnable: Runnable, name: String): SchedulerThread = new SchedulerThread(runnable,name)
  def apply(runnable: Runnable, name: String,daemon:Boolean): SchedulerThread = new SchedulerThread(runnable, name,daemon)
  def daemon(name:String,runnable:Runnable):SchedulerThread = {
    SchedulerThread(runnable,name,true)
  }
  def nonDaemon(name:String,runnable:Runnable):SchedulerThread={
   SchedulerThread(runnable,name)
  }
}
class SchedulerThread(runnable: Runnable,name:String) extends Thread(runnable,name){
  def this(runnable: Runnable,name:String,daemon:Boolean){
    this(runnable,name)
    configureThread(daemon)
  }
  def configureThread(daemon:Boolean): Unit ={
    setDaemon(daemon)
    setUncaughtExceptionHandler(new UncaughtExceptionHandler {
      override def uncaughtException(t: Thread, e: Throwable): Unit = {
        // TODO: 对日志进行打印 
      }
    })
  }
}
