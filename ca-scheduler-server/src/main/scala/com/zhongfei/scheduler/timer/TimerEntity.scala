package com.zhongfei.scheduler.timer

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior, ReplyEffect}
import akka.persistence.typed.{PersistenceId, RecoveryCompleted}
import com.zhongfei.scheduler.timer.TimerEntity._

object TimerEntity {


  /**
   * 命令接口
   *
   * @tparam Reply
   */
  sealed trait Command[Reply <: CommandReply] {
    def replyTo: ActorRef[Reply]
  }


  //定时器值对象接口
  trait Timer {
    def applyCommand(cmd: Command[_]): ReplyEffect[Event, Timer]

    def applyEvent(event: Event): Timer

    def fetchEvent(): List[Event]
  }
  //返回的命令类型
  sealed trait CommandReply

  sealed trait OperationResult extends CommandReply

  case object Succeeded extends OperationResult

  final case class Failed(cause: Throwable)

  final case class ScheduleAdd(actionId: Long, body: ScheduleAddBody, expire: Long, timestamp: Long, replyTo: ActorRef[OperationResult]) extends Command[OperationResult]

  case class ScheduleAddBody(appName: String, eventName: String, extra: String)

  final case class ScheduleDel(actionId: Long, replyTo: ActorRef[OperationResult]) extends Command[OperationResult]

  sealed trait Event

  final case class ScheduleAdded(actionId: Long, body: ScheduleAddBody, expire: Long, timestamp: Long) extends Event

  final case class ScheduleDeleted(actionId: Long) extends Event

  final case class TimerActive(timer:Timer,replyTo:ActorRef[OperationResult]) extends Command[OperationResult]
  final case class TimerActived(timer: Timer) extends Event


  def apply(persistenceId: PersistenceId): Behavior[Command[_]] = Behaviors.setup { context => new TimerEntity(persistenceId, context).timer()
  }


}

/**
 * 定时器实体
 */
class TimerEntity(persistenceId: PersistenceId, context: ActorContext[Command[_]]) {


  def timer(): Behavior[Command[_]] =
    EventSourcedBehavior.withEnforcedReplies[Command[_], Event, Timer](persistenceId, InitializeTimer(Map.empty), (state, cmd) => state.applyCommand(cmd), (state, event) => state.applyEvent(event))
    .receiveSignal { msg=>
      //事件都是先增加，再删除，重放时会把老的事件加载进来，但是事件已经过期，这样会立即触发，造成每次重访都会重新触发一遍所有事件，所以需要现在内存中过滤调已经完成的事件，再添加到当前到状态中。
      msg match {
        case (state, RecoveryCompleted) =>
          context.log.debug("重放完成，将初始化定时器转换为活跃定时器")
          val activeState = ActiveTimer(TimingWheelStorage())
          state.fetchEvent().foreach(activeState.applyEvent)
          context.self ! TimerActive(state,null)
      }

    }



  case class InitializeTimer(storage: Map[Long, Event]) extends Timer {
    override def applyCommand(cmd: Command[_]): ReplyEffect[Event, Timer] = {
      context.log.debug(s"初始化定时器接收到命令,$cmd")
      cmd match {
        case ScheduleAdd(actionId, body, expire, timestamp, replyTo) =>
          Effect.persist(ScheduleAdded(actionId, body, expire, timestamp)).thenReply(replyTo)(_ => Succeeded)
        case ScheduleDel(actionId, replyTo) =>
          Effect.persist(ScheduleDeleted(actionId)).thenReply(replyTo)(_ => Succeeded)
        case TimerActive(timer,replyTo) =>Effect.persist(TimerActived(timer)).thenNoReply()
      }
    }


    override def applyEvent(event: Event): Timer = {
      context.log.debug(s"初始化定时器接收到事件,$event")
      event match {
        case add@ScheduleAdded(actionId, body, expire, timestamp) =>
          copy(storage + (actionId -> add))
        case ScheduleDeleted(actionId) =>
          copy(storage - actionId)
        case TimerActived(timer) =>
          context.log.debug("将状态转换为活跃定时器")
          timer
      }
    }

    override def fetchEvent(): List[Event] = storage.values.toList

  }

  case class ActiveTimer(timerStorage: TimingWheelStorage) extends Timer {
    override def applyCommand(cmd: Command[_]): ReplyEffect[Event, Timer] = {
      context.log.debug(s"活跃定时器收到命令$cmd")
      cmd match {
        case ScheduleAdd(actionId, body, expire, timestamp, replyTo) =>
          Effect.persist(ScheduleAdded(actionId, body, expire, timestamp)).thenReply(replyTo)(_ => Succeeded)
        case ScheduleDel(actionId, replyTo) =>
          Effect.persist(ScheduleDeleted(actionId)).thenReply(replyTo)(_ => Succeeded)
      }
    }

    override def applyEvent(event: Event): Timer = {
      context.log.debug(s"活跃定时器接收到事件$event")
      event match {
        case ScheduleAdded(actionId, body, expire, timestamp) =>
          val realExpire = expire - (System.currentTimeMillis() - timestamp)
          timerStorage.save(ScheduleExecutor(actionId, realExpire, body))
          this
        case ScheduleDeleted(actionId) =>
          timerStorage.delete(actionId)
          this
      }
    }

    override def fetchEvent(): List[Event] = Nil
  }


}
