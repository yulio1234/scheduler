package com.zhongfei.scheduler.convert

import com.zhongfei.scheduler.Converter
import com.zhongfei.scheduler.registry.ApplicationManager.{AppRegisterRequest, AppUnregisterRequest, Command}
import com.zhongfei.scheduler.transport.Peer
import com.zhongfei.scheduler.transport.protocol.SchedulerProtocol.{ActionTypeEnum, Protocol, Request, Response}

/**
 * 应用管理器消息转换器
 */
object AppManagerCmdConverter extends Converter[Protocol,Command,Peer]{

  override def convert(message: Protocol, attachment: Peer): Command = {
    message match {
      case Request(_, _, _, actionId, actionType, _, _, _,appName)=>
        actionType match {
          case ActionTypeEnum.Register => AppRegisterRequest(actionId,new String(appName),attachment)
          case ActionTypeEnum.Unregister => AppUnregisterRequest(actionId,new String(appName),attachment)
        }
      case Response(magic, version, protocolType, actionId, success, errorCode, timestamp) =>  _
    }
  }
}
