package com.zhongfei.scheduler.registry

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import com.zhongfei.scheduler.registry.ApplicationManager.{Command, GroupTerminated, HeartbeatRequest, Unregister, Unregistered}
import com.zhongfei.scheduler.transport.protocol.SchedulerProtocol.Protocol
import com.zhongfei.scheduler.transport.{Node, Peer}
object ApplicationManager{
  trait Command extends Protocol
  trait Response extends Command
  //应用心跳检测请求
  case class HeartbeatRequest(actionId:Long,appName:String,peer: Peer)
    extends ApplicationManager.Command with ApplicationGroup.Command with Application.Command
  //应用取消注册请求
  case class Unregister(actionId:Long,appName:String,peer: Peer)
    extends ApplicationManager.Command with ApplicationGroup.Command with Application.Command

  case class Unregistered(actionId:Long) extends Response
  //应用组关闭事件
  case class GroupTerminated(appGroupName:String) extends Command
  def apply(node:Node): Behavior[Command] = Behaviors.setup(context => new ApplicationManager(node,context).manage(Map.empty))
}
/**
 * 应用管理器
 * @param context
 */
private class ApplicationManager(node:Node,context:ActorContext[Command]){
  private def manage(appGroupMap:Map[String,ActorRef[ApplicationGroup.Command]]): Behavior[Command] = Behaviors.receiveMessage[Command]{message=>
    message match {
      case request @ HeartbeatRequest(_, appName,_) =>
        appGroupMap.get(appName) match {
            //如果有应用组就转发消息
          case Some(actor) => actor ! request
            Behaviors.same
            //如果没有应用组，就先创建在转发
          case None =>
            context.log.info("没有查询到应用组，创建一个新的应用组 {}",appName)
            //创建应用组
            val appGroupActor = context.spawn(ApplicationGroup(appName),"appGroup-"+appName)
            //监听应用组关闭事件
            context.watchWith(appGroupActor,GroupTerminated(appName))
            appGroupActor ! request
            manage(appGroupMap +(appName -> appGroupActor))

        }
      case request @ Unregister(actionId, appName, peer) =>
        context.log.info(s"注销应用请求，没有找到相应的应用组，客户端信息=$peer,服务器信息=$node")
        appGroupMap.get(appName) match {
          case Some(actor) =>
            actor ! request
            Behaviors.same
          // TODO: 应用需要应答消息，需要创建一个专门用于应答的actor
          case None =>
//            context.spawnAnonymous()
            Behaviors.same
        }
        //匹配应用组注销消息，并将数据清楚清单
      case GroupTerminated(appGroupName) =>
        context.log.info("接收到应用组注销消息：appGroupName=",appGroupName)
        manage(appGroupMap - appGroupName)
    }
  }
}
