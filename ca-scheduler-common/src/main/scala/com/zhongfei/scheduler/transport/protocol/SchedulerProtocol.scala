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
    val Request,Response = Value
  }


  /**
   * 动作类型enum
   */
  object ActionTypeEnum extends Enumeration {
    val HeartBeat,Unregister,AddSchedule,DeleteSchedule = Value
  }
  //协议标记
  trait Protocol extends Message
  //请求协议
  case class Request(magic:Byte=magic,
                     version:Byte=version,
                     protocolType:Byte=ProtocolTypeEnum.Request.id.toByte,
                     actionId:Long,
                     actionType:Byte,
                     timestamp:Long=System.currentTimeMillis(),
                     expire:Long = -1,
                     length:Short = -1,
                     context:Array[Byte] = null) extends Protocol
  //响应协议
  case class Response(magic:Byte=magic,
                      version:Byte=version,
                      protocolType:Byte = ProtocolTypeEnum.Response.id.toByte,
                      actionId:Long,success:Boolean=true,
                      errorCode:Byte = -1,
                      timestamp:Long = System.currentTimeMillis(),
                      length:Int = -1,
                      content:Array[Byte] = null
                     ) extends Protocol
}
