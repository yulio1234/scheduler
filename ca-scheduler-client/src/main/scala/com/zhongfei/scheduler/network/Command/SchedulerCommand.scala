package com.zhongfei.scheduler.network.Command

import com.zhongfei.scheduler.network.SchedulerConnection.Event
import com.zhongfei.scheduler.network.SchedulerConnectionManager

/**
 * 调度服务器命令集
 */
object SchedulerCommand {
  //命令（请求）
  //应用心跳检测请求
  case class HeartBeat(actionId:Long,appName:String)

  //应用取消注册请求
  case class Unregister(actionId:Long,appName:String)


  //事件（响应）
  case class HeartBeaten(actionId:Long) extends Event with SchedulerConnectionManager.Event
  //应用取消注册成功
  case class Unregistered(actionId:Long) extends Event with SchedulerConnectionManager.Event
}
