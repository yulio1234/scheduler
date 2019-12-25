package com.zhongfei.scheduler.timer

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import akka.cluster.sharding.typed.scaladsl.EntityTypeKey
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior, ReplyEffect}
import akka.persistence.typed.{PersistenceId, RecoveryCompleted}
import com.zhongfei.scheduler.timer.TimerEntity._
import com.zhongfei.scheduler.transfer.{CommandReply, OperationResult}
import com.zhongfei.scheduler.transport.Peer
object TimerEntity {
   val timerTypeKey: EntityTypeKey[Command[_]] = EntityTypeKey[Command[_]]("Timer")

  /**
   * 命令接口
   *
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

    def enabled(boolean: Boolean):Unit
  }
  case object Success extends OperationResult
  final case class ScheduleAdd(actionId: Long, body: ScheduleAddBody, expire: Long, timestamp: Long,peer: Peer, replyTo: ActorRef[OperationResult])
    extends Command[OperationResult]

  case class ScheduleAddBody(appName: String, eventName: String, extra: String)

  final case class ScheduleDel(actionId: Long, peer: Peer,replyTo: ActorRef[OperationResult]) extends Command[OperationResult]

  sealed trait Event extends CborSerializable

  final case class ScheduleAdded(actionId: Long, body: ScheduleAddBody, expire: Long, timestamp: Long) extends Event

  final case class ScheduleDeleted(actionId: Long) extends Event

  def apply(persistenceId: PersistenceId): Behavior[Command[_]] = Behaviors.setup { context => new TimerEntity(persistenceId, context).timer()
  }


}

/**
 * 定时器实体
 */
class TimerEntity(persistenceId: PersistenceId, context: ActorContext[Command[_]]) {


  def timer(): Behavior[Command[_]] =
    EventSourcedBehavior.withEnforcedReplies[Command[_], Event, Timer](persistenceId, TimingWheelTimer(TimingWheelStorage()), (state, cmd) => state.applyCommand(cmd), (state, event) => state.applyEvent(event))
    .receiveSignal { msg=>
      msg match {
        case (state, RecoveryCompleted) =>
          context.log.debug("重放完成，将初始化定时器转换为活跃定时器")
          state.enabled(true)
      }

    }

  case class TimingWheelTimer(timerStorage: TimingWheelStorage) extends Timer {

    override def enabled(boolean: Boolean): Unit = {
      if(boolean){
        timerStorage.start()
      }else{
        timerStorage.shutdown()
      }
    }

    override def applyCommand(cmd: Command[_]): ReplyEffect[Event, Timer] = {
      context.log.debug(s"活跃定时器收到命令$cmd")
      cmd match {
        case ScheduleAdd(actionId, body, expire, timestamp, peer,replyTo) =>
          Effect.persist(ScheduleAdded(actionId, body, expire, timestamp)).thenReply(replyTo)(_ => Success)
        case ScheduleDel(actionId, peer,replyTo) =>
          Effect.persist(ScheduleDeleted(actionId)).thenReply(replyTo)(_ => Success)
      }
    }

    override def applyEvent(event: Event): Timer = {
      context.log.debug(s"活跃定时器接收到事件$event")
      event match {
        case ScheduleAdded(actionId, body, expire, timestamp) =>
          val currentTimestamp = System.currentTimeMillis()
          //计算当前时间和命令发送时间时间差，以减少误差
          val realExpire = expire - (currentTimestamp - timestamp)

          context.log.debug(s"命令发送时间：$timestamp,当前时间$currentTimestamp,原始偏移量：$expire,当前到期时间偏移量，$realExpire")
          timerStorage.save(ScheduleExecutor(actionId, if(realExpire > 0) realExpire else 0, body))
          this
        case ScheduleDeleted(actionId) =>
          context.log.debug(s"接收到删除调度任务命令 actionId：$actionId")
          timerStorage.delete(actionId)
          this
      }
    }

    override def fetchEvent(): List[Event] = Nil
  }


}
