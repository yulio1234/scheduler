package com.zhongfei.scheduler.Exception

/**
 * 调度系统异常类
 */
object SchedulerExceptionFactory{
  case class UnknownException(code:Int = 0,message :String = "未知异常") extends SchedulerException(code,message)
  case class ProtocolMagicException(code:Int = 1,message:String = "协议魔数不匹配异常") extends SchedulerException(code,message)
  case class ProtocolVersionException(code:Int = 2,message:String = "协议版本异常") extends SchedulerException(code,message)
  case class UnregisterException(code: Int = 3, message: String="注销应用失败异常") extends SchedulerException(code,message)
  case class NetworkTransferException(code :Int =4 ,message :String = "网络传输异常",cause:Throwable) extends SchedulerException(code,message,cause){
    def this(cause:Throwable){
      this(4 ,"网络传输异常",cause)
    }
    def this(){
      this(4 ,"网络传输异常",null)
    }
  }
  val exceptions:Map[Int,SchedulerException] = Map.empty
  exceptions + (ProtocolMagicException().code -> ProtocolMagicException())
  exceptions + (ProtocolVersionException().code -> ProtocolVersionException())
  exceptions + (UnregisterException().code -> UnregisterException())
  exceptions + (new NetworkTransferException().code -> new NetworkTransferException())

  def get(code:Int): SchedulerException ={
    exceptions.get(code) match {
      case Some(value) => value
      case None =>UnknownException()
    }
  }
}

