package com.zhongfei.scheduler.timer

import akka.actor.typed.scaladsl.{ActorContext, Behaviors, Routers, TimerScheduler}
import akka.actor.typed.{ActorRef, Behavior}
import com.zhongfei.scheduler.network.Application.{ScheduleExpire, ScheduleExpired}
import com.zhongfei.scheduler.network.ApplicationManager
import com.zhongfei.scheduler.network.ApplicationManager.{ApplicationRef, SelectAnApplication}
import com.zhongfei.scheduler.timer.ScheduleBroker.{Command, Expire, Terminate, WrappedApplicationRef}
import com.zhongfei.scheduler.timer.TimerEntity.{ScheduleBody, ScheduleDo, ScheduleRedo}

import scala.concurrent.duration._

object ScheduleBroker {

  trait Command

  case object Expire extends Command

  case object Terminate extends Command

  case class WrappedApplicationRef(option: Option[ApplicationRef]) extends Command

  case class WrappedScheduleExpired(scheduleExpired: ScheduleExpired) extends Command

  def apply(actorRef: ActorRef[TimerEntity.Command], scheduleBody: ScheduleBody): Behavior[Command] =
    Behaviors.setup { context => Behaviors.withTimers { timers => new ScheduleBroker(timers, actorRef, scheduleBody, context).broker() } }
}

class ScheduleBroker(timers: TimerScheduler[Command], actorRef: ActorRef[TimerEntity.Command], scheduleBody: ScheduleBody, context: ActorContext[Command]) {
  private val adapter: ActorRef[Option[ApplicationRef]] = context.messageAdapter(WrappedApplicationRef)

  def broker(): Behavior[Command] = Behaviors.receiveMessage[Command] {
    case Expire =>
      //通知客户端调度结束
      val group = Routers.group(ApplicationManager.selectAnApplication)
      context.spawnAnonymous(group) ! SelectAnApplication(scheduleBody.domain, adapter)
      Behaviors.same
    case WrappedApplicationRef(option) =>
      option match {
        case Some(value) =>
          //设置执行到期时间
          timers.startSingleTimer(Terminate, Terminate, value.processTimeout.millis)
          value.replyTo ! ScheduleExpire(scheduleBody, value.processTimeout.millis, context.self)
        case None => Behaviors.stopped
      }
      Behaviors.same
    case ScheduleExpired(success) =>
      if (success) {
        actorRef ! ScheduleDo(scheduleBody.id)
        Behaviors.stopped
      } else {
        actorRef ! ScheduleRedo(scheduleBody)
        Behaviors.stopped
      }
    case Terminate =>
      actorRef ! ScheduleRedo(scheduleBody)
      Behaviors.stopped
  }
}
