package com.zhongfei.scheduler.Exception

class SchedulerException(code:Int,message:String,cause:Throwable) extends RuntimeException(message,cause){
    def this(code:Int,message:String){
      this(code,message,null)
    }
  }