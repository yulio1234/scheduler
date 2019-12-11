package com.zhongfei.scheduler.command

import akka.actor.typed.ActorRef
import com.zhongfei.scheduler.Message
import com.zhongfei.scheduler.network.{Application, ApplicationDispatcher, ApplicationGroup, ApplicationManager}
import com.zhongfei.scheduler.transport.Peer
import com.zhongfei.scheduler.network.ServerDispatcher
/**
 * 调度服务器命令集
 */
object SchedulerCommand {
  trait Command extends Message
  trait Event extends Message
  //命令（请求）
  //应用心跳检测请求
  case class HeartBeat(actionId:Long,appName:String,peer: Peer,replyTo:ActorRef[ApplicationDispatcher.Event])
    extends ApplicationManager.Command with ApplicationGroup.Command with Application.Command with ServerDispatcher.Command
  //应用取消注册请求
  case class Unregister(actionId:Long,appName:String,peer: Peer,reply:ActorRef[ApplicationDispatcher.Event])
    extends ApplicationManager.Command with ApplicationGroup.Command with Application.Command

  //事件（响应）
  case class HeartBeaten(actionId:Long,hosts:Option[String]) extends ApplicationDispatcher.Event
  //应用取消注册成功
  case class Unregistered(actionId:Long) extends ApplicationDispatcher.Event
}
