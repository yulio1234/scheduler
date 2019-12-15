package com.zhongfei.scheduler.network

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import com.zhongfei.scheduler.command.SchedulerCommand
import com.zhongfei.scheduler.command.SchedulerCommand.{HeartBeat, Unregister, Unregistered}
import com.zhongfei.scheduler.network.ApplicationManager.{Command, GroupTerminated}
import com.zhongfei.scheduler.options.SingletonOption
object ApplicationManager{
  trait Message
  trait Command extends Message
  trait Event extends Message
  //应用组关闭事件
  case class GroupTerminated(appGroupName:String) extends Command
  def apply(option:SingletonOption): Behavior[Command] = Behaviors.setup(context => new ApplicationManager(option,context).manage(Map.empty))
}
/**
 * 应用管理器
 * @param context
 */
private class ApplicationManager(option:SingletonOption,context:ActorContext[Command]){
  private def manage(appGroupMap:Map[String,ActorRef[ApplicationGroup.Command]]): Behavior[Command] = Behaviors.receiveMessage[Command]{
      case command @ HeartBeat(_, appName,_,_) =>
        appGroupMap.get(appName) match {
            //如果有应用组就转发消息
          case Some(actor) => actor ! command
            Behaviors.same
            //如果没有应用组，就先创建在转发
          case None =>
            context.log.info("没有查询到应用组，创建一个新的应用组 {}",appName)
            //创建应用组
            val appGroupActor = context.spawn(ApplicationGroup(option,appName),"application-group-"+appName)
            //监听应用组关闭事件
            context.watchWith(appGroupActor,GroupTerminated(appName))
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
            replyTo ! Unregistered(actionId)
            Behaviors.same
        }
        //匹配应用组注销消息，并将数据清除
      case GroupTerminated(appGroupName) =>
        context.log.info("接收到应用组注销消息：appGroupName=",appGroupName)
        manage(appGroupMap - appGroupName)
  }
}
