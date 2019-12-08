package com.zhongfei.scheduler.network

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import com.zhongfei.scheduler.command.SchedulerCommand.{HeartBeat, Unregister, Unregistered}
import com.zhongfei.scheduler.network.ApplicationGroup.ApplicationTerminated
import com.zhongfei.scheduler.options.SingletonOption

object ApplicationGroup{
  trait Command
  trait Event
  case object ApplicationTerminated extends Command
  /**
   * 应用组
   * @param appName
   * @return
   */
  def apply(option:SingletonOption,appName: String): Behavior[Command] = Behaviors.setup(context => new ApplicationGroup(option,context,appName).handle(Map.empty))
}
private class ApplicationGroup(option:SingletonOption,context:ActorContext[ApplicationGroup.Command],appName:String){

  def handle(appMap:Map[String,ActorRef[Application.Command]]):Behavior[ApplicationGroup.Command] = Behaviors.receiveMessage[ApplicationGroup.Command] { message => {
    message match {
        //处理应用注册消息
      case command @ HeartBeat(_, _ , peer) =>
        //检查是否由存在的应用，如果有就返回成功，没有就创建一个
        val uri:String = peer.uri()
        appMap.get(uri) match {
          case Some(application) =>
            //将消息转发给应用
            application ! command
            //如果没有找到应用，就创建一个新的应用
          case None =>
            context.log.info("没有查询到应用，创建一个新的应用 {}",uri)
            //创建应用
            val application = context.spawn(Application(option,peer),s"application-${appName}-${uri}")
            //监控应用崩溃
            context.watchWith(application,ApplicationTerminated)
            application ! command
            //保存应用
            handle(appMap + (uri -> application))

        }
        context.log.info(s"检测到应用注册消息 :$peer")
        Behaviors.same
      case unregister @ Unregister(actionId,_,peer,reply) =>

        appMap.get(peer.uri()) match {
          case Some(appActor) => appActor ! unregister
          case None => reply ! Unregistered(actionId)
        }
        Behaviors.same
    }
  }}
}
