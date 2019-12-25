package com.zhongfei.scheduler.network

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import com.zhongfei.scheduler.network.ApplicationManager._
import com.zhongfei.scheduler.options.ServerOption
import com.zhongfei.scheduler.transfer.{CommandReply, OperationResult}
import com.zhongfei.scheduler.transport.Peer
object ApplicationManager{
  sealed trait Command[Reply <: CommandReply] {
    def replyTo: ActorRef[Reply]
  }
  //命令（请求）
  //应用心跳检测请求
  case class HeartBeat(actionId:Long,appName:String,peer: Peer,replyTo:ActorRef[OperationResult])
    extends Command[OperationResult]  with ApplicationGroup.Command with Application.Command
  //事件（响应）
  case class HeartBeaten(actionId:Long) extends OperationResult
  //应用取消注册成功
  case class Unregistered(actionId:Long) extends OperationResult
  //应用取消注册请求
  case class Unregister(actionId:Long,appName:String,peer: Peer,replyTo:ActorRef[OperationResult])
    extends Command[OperationResult]  with ApplicationGroup.Command with Application.Command
  //查询应用集合
  case class ApplicationListQuery(appName:String,replyTo:ActorRef[OperationResult]) extends Command[OperationResult] with ApplicationGroup.Command
  //应用集合查询完毕
  case class CurrentApplicationList(list: List[ActorRef[Application.Command]]) extends OperationResult
  //应用组关闭事件
  case class GroupTerminated(appGroupName:String,replyTo:ActorRef[Nothing]) extends Command[Nothing]
  def apply(option:ServerOption): Behavior[Command[_]] = Behaviors.setup(context => new ApplicationManager(option,context).manage(Map.empty))
}
/**
 * 应用管理器
 * @param context
 */
private class ApplicationManager(option:ServerOption,context:ActorContext[Command[_]]){
  private def manage(appGroupMap:Map[String,ActorRef[ApplicationGroup.Command]]): Behavior[Command[_]] = Behaviors.receiveMessage[Command[_]]{
      case command @ HeartBeat(_, appName,_,_) =>
        appGroupMap.get(appName) match {
            //如果有应用组就转发消息
          case Some(actor) => actor ! command
            Behaviors.same
            //如果没有应用组，就先创建在转发
          case None =>
            context.log.info("没有查询到应用组，创建一个新的应用组 group:{}",appName)
            //创建应用组
            val appGroupActor = context.spawnAnonymous(ApplicationGroup(option,appName))
            //监听应用组关闭事件
            context.watchWith(appGroupActor,GroupTerminated(appName,null))
            appGroupActor ! command
            manage(appGroupMap + (appName -> appGroupActor))

        }
      case command @ Unregister(actionId, appName, peer,replyTo) =>
        context.log.info(s"注销应用请求，没有找到相应的应用组，客户端信息=$peer,服务器信息=${option.node}")
        appGroupMap.get(appName) match {
          case Some(actor) =>
            actor ! command
            Behaviors.same
          case None =>
            replyTo !  Unregistered(actionId)
            Behaviors.same
        }
      case query @ ApplicationListQuery(appName,replyTo) =>
        appGroupMap.get(appName) match {
          case Some(actorRef) =>
            actorRef !  query
            Behaviors.same
          case None =>
            replyTo ! CurrentApplicationList(Nil)
            Behaviors.same
        }
        //匹配应用组注销消息，并将数据清除
      case GroupTerminated(appGroupName,_) =>
        context.log.info("接收到应用组注销消息：appGroupName : ",appGroupName)
        manage(appGroupMap - appGroupName)
  }
}
