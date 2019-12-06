package com.zhongfei.scheduler.transport

/**
 * 调度系统异常类
 */
object SchedulerExceptions{
  case class ProtocolMagicException(code:Int = 1,message:String = "协议魔数不匹配异常") extends RuntimeException(message)
  case class ProtocolVersionException(code:Int = 2,message:String = "协议版本异常") extends RuntimeException(message)
  case class AppUnregisterException(code: Int = 3, message: String="注销应用失败异常") extends RuntimeException(message)
  //case class RepeaterChannelWriteException(override val code: Long = 2, override val message: String = "消息转发失败") extends SchedulerException
}

