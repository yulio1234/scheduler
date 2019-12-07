package com.zhongfei.scheduler.registry

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import com.zhongfei.scheduler.registry.ApplicationGroup.Command
import com.zhongfei.scheduler.registry.ApplicationManager.AppRegisterRequest

object ApplicationGroup{
  trait Command
  case object AppTerminated extends Command
  /**
   * 应用组
   * @param appName
   * @return
   */
  def apply(appName: String): Behavior[Command] = Behaviors.setup(context => new ApplicationGroup(context,appName).handle(Map.empty))
}
private class ApplicationGroup(context:ActorContext[ApplicationGroup.Command],appName:String){

  def handle(appMap:Map[String,ActorRef[Application.Command]]):Behavior[Command] = Behaviors.receiveMessage[Command] { message => {
    message match {
        //处理应用注册消息
      case request @ AppRegisterRequest(actionId, appName, peer) =>
        //检查是否由存在的应用，如果有就返回成功，没有就创建一个
        appMap.get(peer.uri()) match {
          case Some(application) =>
            //将消息转发给应用
            application ! request

          case None =>
        }
        context.log.info(s"检测到应用注册消息 :$peer")

//        context.spawn(Application(peer))
        Behaviors.same
    }
  }}
}
