package com.zhongfei.scheduler.timer

import akka.actor.typed.{ActorRef, Behavior, PostStop}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, Routers}
import akka.persistence.typed.scaladsl.Effect
import com.zhongfei.scheduler.network.ApplicationManager
import com.zhongfei.scheduler.network.ApplicationManager.{ApplicationRef, SelectAnApplication}
import com.zhongfei.scheduler.timer.ScheduleBroker.{Command, WrappedApplicationRef}
import com.zhongfei.scheduler.timer.TimerEntity.{ScheduleDo, ScheduleExpire}
object ScheduleBroker{
  trait Command
  case class WrappedApplicationRef(option:Option[ApplicationRef])extends Command
}
class ScheduleBroker(actorRef: ActorRef[ScheduleDo],scheduleDo: ScheduleDo,context:ActorContext[Command]) {
  private val adapter: ActorRef[Option[ApplicationRef]] = context.messageAdapter(WrappedApplicationRef)
  def broker(): Behavior[Command] = Behaviors.receiveMessage{
    case ScheduleDo(body) =>
      //通知客户端调度结束
      val group = Routers.group(ApplicationManager.selectAnApplication)
      context.spawnAnonymous(group) ! SelectAnApplication(body.domain,adapter)
      Behaviors.same
    case WrappedApplicationRef(option) =>
      option match {
        case Some(value) =>
        case None => Behaviors.stopped
      }
      Behaviors.same
  }.receiveSignal{
        //关闭应用前把消息还回去
    case (context,PostStop) =>
      actorRef ! scheduleDo
      Behaviors.same
  }
}
