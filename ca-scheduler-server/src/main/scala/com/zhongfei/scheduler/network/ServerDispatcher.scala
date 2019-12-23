package com.zhongfei.scheduler.network

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import com.zhongfei.scheduler.network.ServerDispatcher.{Command, HeartBeat, OperationResult, Unregister}
import com.zhongfei.scheduler.options.SingletonOption
import com.zhongfei.scheduler.transport.Peer

/**
 * 调度处理器处理器
 */
object ServerDispatcher {

  sealed trait Command[Reply <: CommandReply] {
    def replyTo: ActorRef[Reply]
  }
  //返回的命令类型
  sealed trait CommandReply

  sealed trait OperationResult extends CommandReply with ApplicationDispatcher.Command
  //命令（请求）
  //应用心跳检测请求
  case class HeartBeat(actionId:Long,appName:String,peer: Peer,replyTo:ActorRef[OperationResult])
    extends Command[OperationResult] with ApplicationManager.Command with ApplicationGroup.Command with Application.Command
  //应用取消注册请求
  case class Unregister(actionId:Long,appName:String,peer: Peer,replyTo:ActorRef[OperationResult])
    extends Command[OperationResult] with ApplicationManager.Command with ApplicationGroup.Command with Application.Command

  //事件（响应）
  case class HeartBeaten(actionId:Long) extends OperationResult
  //应用取消注册成功
  case class Unregistered(actionId:Long) extends OperationResult

  final case class ScheduleAdd(actionId: Long, body: ScheduleAddBody, expire: Long, timestamp: Long,peer: Peer, replyTo: ActorRef[OperationResult])
    extends Command[OperationResult] with TimerDispatcher.Command

  case class ScheduleAddBody(appName: String, eventName: String, extra: String)
  //与应用通讯的消息的消息
  def apply(option: SingletonOption): Behavior[Command[_]] = Behaviors.setup{ context => new ServerDispatcher(option,context).process()}
}

/**
 * 全局消息处理器，负责分发各种消息
 * @param option
 */
private class ServerDispatcher(option:SingletonOption, context:ActorContext[Command[_]])  {
  //创建应用管理者
  val applicationManager = context.spawn(ApplicationManager(option),"applicationManager")

  // TODO:  进行查询数据保存
  def process(): Behavior[Command[_]] = Behaviors.receiveMessage[Command[_]]{message => {
    message match {
      case heartbeat @ HeartBeat(_, _, peer:Peer,_) =>
        val actorRef: ActorRef[OperationResult] = buildExtra(message, peer)
        //转发给应用管理器
        applicationManager ! heartbeat.copy(replyTo = actorRef)
        Behaviors.same
        //匹配注销应用请求，并转发
      case unregister @ Unregister(_,_,peer,_) =>
        val actorRef = buildExtra(message, peer)
        applicationManager ! unregister.copy(replyTo = actorRef)
        Behaviors.same
    }
  }}
  def buildExtra(command:Command[_],peer: Peer): ActorRef[OperationResult] ={
    context.spawnAnonymous(ApplicationDispatcher(option, peer, command))
  }
}
