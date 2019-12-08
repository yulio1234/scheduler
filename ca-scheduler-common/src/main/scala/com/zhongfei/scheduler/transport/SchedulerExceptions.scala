package com.zhongfei.scheduler.transport

/**
 * 调度系统异常类
 */
object SchedulerExceptions{
  case class ProtocolMagicException(code:Int = 1,message:String = "协议魔数不匹配异常") extends RuntimeException(message)
  case class ProtocolVersionException(code:Int = 2,message:String = "协议版本异常") extends RuntimeException(message)
  case class UnregisterException(code: Int = 3, message: String="注销应用失败异常") extends RuntimeException(message)
  case class NetworkTransferException(code :Int =4 ,message :String = "网络传输异常",cause:Throwable) extends RuntimeException(message,cause){
    def this(cause:Throwable){
      this(4 ,"网络传输异常",cause)
    }
  }
}

