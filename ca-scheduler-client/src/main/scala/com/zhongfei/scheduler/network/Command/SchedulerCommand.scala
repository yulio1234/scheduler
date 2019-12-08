package com.zhongfei.scheduler.network.Command

import com.zhongfei.scheduler.Message
import com.zhongfei.scheduler.transport.Peer

/**
 * 调度服务器命令集
 */
object SchedulerCommand {
  trait Command extends Message
  trait Event extends Message
  //命令（请求）
  //应用心跳检测请求
  case class HeartBeat(actionId:Long,appName:String,peer: Peer)

  //应用取消注册请求
  case class Unregister(actionId:Long,appName:String,peer: Peer)


  //事件（响应）
  case class HeartBeaten(actionId:Long,hosts:String)
  //应用取消注册成功
  case class Unregistered(actionId:Long)
}
