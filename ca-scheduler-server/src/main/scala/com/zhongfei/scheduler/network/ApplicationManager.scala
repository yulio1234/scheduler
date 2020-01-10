package com.zhongfei.scheduler.network

import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import com.zhongfei.scheduler.network.Application.ScheduleExpire
import com.zhongfei.scheduler.network.ApplicationManager._
import com.zhongfei.scheduler.network.ServerDispatcher.WrappedScheduleExpire
import com.zhongfei.scheduler.options.ServerOption
import com.zhongfei.scheduler.transfer.OperationResult
import com.zhongfei.scheduler.transport.Peer
import com.zhongfei.scheduler.transport.protocol.ApplicationOption
object ApplicationManager{
  trait Command
  //命令（请求）
  //应用心跳检测请求
  case class HeartBeat(applicationOption: ApplicationOption,replyTo:ActorRef[OperationResult])
    extends Command with ApplicationGroup.Command with Application.Command
  //事件（响应）
  case class HeartBeaten(actionId:Long) extends OperationResult
  //应用取消注册成功
  case class Unregistered(actionId:Long) extends OperationResult
  //应用取消注册请求
  case class Unregister(actionId:Long,appName:String,peer: Peer,replyTo:ActorRef[OperationResult])
    extends Command  with ApplicationGroup.Command with Application.Command


  //查询应用集合
  case class ApplicationListQuery(appName:String,replyTo:ActorRef[CurrentApplicationList]) extends Command with ApplicationGroup.Command
  //应用集合查询完毕
  case class CurrentApplicationList(list: List[ActorRef[Application.Command]]) extends OperationResult
  //查找一个应用引用
  case class SelectAnApplication(appName:String,replyTo: ActorRef[Option[ApplicationRef]]) extends Command with ApplicationGroup.Command with Application.Command
  case class ApplicationRef(processTimeout:Long,replyTo:ActorRef[ScheduleExpire])
  //应用组关闭事件
  case class GroupTerminated(appGroupName:String) extends Command
  val applicationListKey = ServiceKey[ApplicationListQuery]("queryApplication")
  val selectAnApplication = ServiceKey[SelectAnApplication]("selectAnApplication")
  def apply(option:ServerOption,dispatcher:ActorRef[WrappedScheduleExpire]): Behavior[Command] = Behaviors.setup{context =>
    //注册应用查询事件
    context.system.receptionist ! Receptionist.register(applicationListKey,context.self)
    context.system.receptionist ! Receptionist.register(selectAnApplication,context.self)
    new ApplicationManager(option,dispatcher,context).manage(Map.empty)}
}
/**
 * 应用管理器
 * @param context
 */
private class ApplicationManager(option:ServerOption, dispatcher: ActorRef[WrappedScheduleExpire], context:ActorContext[Command]){
  private def manage(appGroupMap:Map[String,ActorRef[ApplicationGroup.Command]]): Behavior[Command] = Behaviors.receiveMessage[Command]{
      case command @ HeartBeat(applicationOption,_) =>
        appGroupMap.get(applicationOption.applicationName) match {
            //如果有应用组就转发消息
          case Some(actor) => actor ! command
            Behaviors.same
            //如果没有应用组，就先创建在转发
          case None =>
            context.log.info("没有查询到应用组，创建一个新的应用组 group:{}",applicationOption.applicationName)
            //创建应用组
            val appGroupActor = context.spawnAnonymous(ApplicationGroup(option,dispatcher,applicationOption.applicationName))
            //监听应用组关闭事件
            context.watchWith(appGroupActor,GroupTerminated(applicationOption.applicationName))
            appGroupActor ! command
            manage(appGroupMap + (applicationOption.applicationName -> appGroupActor))

        }
      case command @ Unregister(actionId, appName, peer,replyTo) =>
        context.log.debug(s"注销应用请求，没有找到相应的应用组，客户端信息=$peer,服务器信息=${option.node}")
        appGroupMap.get(appName) match {
          case Some(actor) =>
            actor ! command
            Behaviors.same
          case None =>
            replyTo !  Unregistered(actionId)
            Behaviors.same
        }
      case selectAnApplication: SelectAnApplication =>
        context.log.debug(s"收到查找一个应用引用消息：$selectAnApplication")
        appGroupMap.get(selectAnApplication.appName) match {
          case Some(value) => value ! selectAnApplication
          case None => selectAnApplication.replyTo !  None
        }
        Behaviors.same
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
      case GroupTerminated(appGroupName) =>
        context.log.info("接收到应用组注销消息：appGroupName : ",appGroupName)
        manage(appGroupMap - appGroupName)
  }
}
