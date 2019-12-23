package com.zhongfei.scheduler.transport.protocol

import com.zhongfei.scheduler.Message


/**
 * 通讯协议
 */
object SchedulerProtocol{
  //协议标记
  class Protocol(
                  val magic:Byte=magic,
                  val version:Byte=version,
                  val protocolType:Byte,
                  val actionId:Long,
                  val timestamp:Long,
                  val length:Short = 0,
                  val content:Array[Byte] = null
                ) extends Message
  //请求协议
  case class Request(
                      override val actionId:Long,
                      val actionType:Byte,
                      expire:Long = -1,
                      override val timestamp:Long = System.currentTimeMillis(),
                      override val length:Short = 0,
                      override val content:Array[Byte] = null
                     ) extends Protocol(protocolType = ProtocolTypeEnum.Request.id.toByte,actionId = actionId,timestamp = timestamp)
  //响应协议
  case class Response(
                       override val actionId:Long,
                       success:Boolean=true,
                       errorCode:Byte = -1,
                       override val timestamp:Long = System.currentTimeMillis(),
                       override val length:Short = 0,
                       override val content:Array[Byte] = null
                     ) extends Protocol(protocolType = ProtocolTypeEnum.Response.id.toByte,actionId = actionId,timestamp = timestamp)
  //魔数
  val magic:Byte = 0x0079
  //版本号
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
    val HeartBeat,Unregister,ScheduleAdd,ScheduleDel,ScheduleDone,ActiveServerFetch = Value
  }
}
