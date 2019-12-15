package com.zhongfei.scheduler.command

import akka.actor.typed.ActorRef
import com.zhongfei.scheduler.network._
import com.zhongfei.scheduler.transport.Peer
/**
 * 调度服务器命令集
 */
object SchedulerCommand {

  //命令（请求）
  //应用心跳检测请求
  case class HeartBeat(actionId:Long,appName:String,peer: Peer,replyTo:ActorRef[ApplicationDispatcher.Event])
    extends ApplicationManager.Command with ApplicationGroup.Command with Application.Command with ServerDispatcher.Command
  //应用取消注册请求
  case class Unregister(actionId:Long,appName:String,peer: Peer,reply:ActorRef[ApplicationDispatcher.Event])
    extends ServerDispatcher.Command with ApplicationManager.Command with ApplicationGroup.Command with Application.Command

  //事件（响应）
  case class HeartBeaten(actionId:Long) extends ApplicationDispatcher.Event
  //应用取消注册成功
  case class Unregistered(actionId:Long) extends ApplicationDispatcher.Event
}
