package com.zhongfei.scheduler.timer

import akka.actor.typed.ActorRef
import akka.persistence.typed.scaladsl.{Effect, ReplyEffect}

/**
 * 定时器实体
 */
class TimerEntity {


  /**
   * 命令接口
   * @tparam Reply
   */
  sealed trait Command[Reply <: CommandReply] {
    def replyTo: ActorRef[Reply]
  }



  //返回的命令类型
  sealed trait CommandReply
  sealed trait OperationResult extends CommandReply
  case object Succeeded extends OperationResult
  final case class Failed(cause:Throwable)

  final case class ScheduleAdd(actionId:Long,body:ScheduleAddBody,expire:Long,replyTo: ActorRef[OperationResult]) extends Command[OperationResult]
  case class ScheduleAddBody(appName:String,eventName:String,extra:String)
  final case class ScheduleDel(actionId:Long,replyTo: ActorRef[OperationResult]) extends Command[OperationResult]
  sealed trait Event
  final case class ScheduleAdded(actionId:Long,body:ScheduleAddBody,expire:Long)
  final case class  ScheduleDeleted(actionId:Long)
  sealed trait State
  //定时器值对象接口
   trait Timer{
    def applyCommand(cmd:Command[_]):ReplyEffect[Event,Timer]
    def applyEvent():Timer
  }

  class TimingWheelTimer(timerStorage:TimingWheelStorage) extends Timer{
    override def applyCommand(cmd: Command[_]): ReplyEffect[Event, Timer] = cmd match {
      case ScheduleAdd(actionId, body, expire, replyTo) =>
        Effect.persist(ScheduleAdded(actionId,body,expire)).thenReply(replyTo)(_ => Succeeded)
      case ScheduleDel(actionId,replyTo) =>
        Effect.persist(ScheduleDeleted(actionId)).thenReply(replyTo)(_ => Failed())
    }

    override def applyEvent(): Timer = ???
  }
