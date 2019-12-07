package com.zhongfei.scheduler.transport.protocol

import com.zhongfei.scheduler.Message


/**
 * 通讯协议
 */
object SchedulerProtocol{
  val magic:Byte = 0x0079
  val version:Byte = 1

  /**
   * 协议类型enum
   */
  object ProtocolTypeEnum extends Enumeration{
    val Request = Value(0)
    val Response = Value(1)
  }


  /**
   * 动作类型enum
   */
  object ActionTypeEnum extends Enumeration {
    val Register,Unregister,AddSchedule,DeleteSchedule = Value
  }

  trait Protocol extends Message
  case class Request(magic:Byte=magic,version:Byte=version,protocolType:Byte,actionId:Long,actionType:Byte,timestamp:Long,expire:Long,length:Short,appName:Array[Byte]) extends Protocol
  case class Response(magic:Byte=magic,version:Byte=version,protocolType:Byte,actionId:Long,success:Boolean,errorCode:Byte,timestamp:Long) extends Protocol
}
