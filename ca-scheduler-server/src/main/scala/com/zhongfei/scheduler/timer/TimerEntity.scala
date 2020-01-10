package com.zhongfei.scheduler.timer

import akka.actor.typed.scaladsl.{ActorContext, Behaviors, TimerScheduler}
import akka.actor.typed.{ActorRef, Behavior}
import akka.cluster.sharding.typed.scaladsl.EntityTypeKey
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior, ReplyEffect}
import akka.persistence.typed.{PersistenceId, RecoveryCompleted}
import com.zhongfei.scheduler.network.{Application, ApplicationGroup, ApplicationManager, ServerDispatcher}
import com.zhongfei.scheduler.options.ServerOption
import com.zhongfei.scheduler.timer.ScheduleBroker.Expire
import com.zhongfei.scheduler.timer.TimerEntity._
import com.zhongfei.scheduler.transfer.OperationResult
import com.zhongfei.scheduler.transport.Peer

import scala.concurrent.duration.Deadline
object TimerEntity {
   val timerTypeKey: EntityTypeKey[Command] = EntityTypeKey[Command]("Timer")

  /**
   * 命令接口
   */
  sealed trait Command


  //定时器值对象接口
  trait Timer {
    def applyCommand(cmd: Command): ReplyEffect[Event, Timer]

    def applyEvent(event: Event): Timer

    def fetchEvent(): List[Event]

    def enabled(boolean: Boolean):Unit
  }
  case object Success extends OperationResult
  case object Reject extends OperationResult
  final case class ScheduleAdd(body: ScheduleBody, expire: Long, timestamp: Long,peer: Peer, replyTo: ActorRef[OperationResult])
    extends Command
  //调度消息到期，立即执行
  case class ScheduleDo(id:Long) extends Command
  //调度消息发送失败，重新执行，
  case class ScheduleRedo(body:ScheduleBody) extends Command
  case class ScheduleDone(id:Long) extends Event
  case class ScheduleBody(id:Long,domain: String, eventName: String, extra: String)
  case object ScheduleAction extends Command
  //到期事件
  case class ScheduleExpire(body:ScheduleBody) extends Command
    with ApplicationManager.Command with ApplicationGroup.Command with Application.Command with ServerDispatcher.Command
  //到期事件响应
  case class ScheduleExpired(body:ScheduleBody) extends Event

  /**
   *
   * @param body 调度消息体
   * @param nextActionTimer 必须执行时间，也就是到这个时间之后，必须执行
   * @param actioning 是否在执行中，如果在执行中，就不再执行
   */
  case class ScheduleContext(body:ScheduleBody,var nextActionTimer:Deadline,var actioning:Boolean = false)
  final case class ScheduleDel(id: Long, peer: Peer,replyTo: ActorRef[OperationResult]) extends Command

  sealed trait Event extends CborSerializable

  final case class ScheduleAdded(body: ScheduleBody, expire: Long, timestamp: Long) extends Event

  final case class ScheduleDeleted(id: Long) extends Event
  case class WrappedOperationResult(operationResult:OperationResult) extends Command
  def apply(persistenceId: PersistenceId,option: ServerOption): Behavior[Command] = Behaviors.setup { context =>
    Behaviors.withTimers {
      timers =>
      new TimerEntity(persistenceId, option,timers, context).timer()
    }
  }


}

/**
 * 定时器实体
 */
class TimerEntity(persistenceId: PersistenceId, option:ServerOption,timers:TimerScheduler[Command],context: ActorContext[Command]) {


  def timer(): Behavior[Command] =
    EventSourcedBehavior.withEnforcedReplies[Command, Event, Timer](persistenceId, TimingWheelTimer(TimingWheelStorage(),Map.empty), (state, cmd) => state.applyCommand(cmd), (state, event) => state.applyEvent(event))
    .receiveSignal { msg=>
      msg match {
        case (state, RecoveryCompleted) =>
          context.log.debug("重放完成，将初始化定时器转换为活跃定时器")
          state.enabled(true)
      }

    }


  case class TimingWheelTimer(timerStorage: TimingWheelStorage,var expireSchedules: Map[Long,ScheduleContext]) extends Timer {
    //定时执行
    timers.startTimerWithFixedDelay(ScheduleAction,option.scheduleExpireActionInterval)
    override def enabled(boolean: Boolean): Unit = {
      if(boolean){
        timerStorage.start()
      }else{
        timerStorage.shutdown()
      }
    }

    override def applyCommand(cmd: Command): ReplyEffect[Event, Timer] = {
      context.log.debug(s"活跃定时器收到命令$cmd")
      cmd match {
        case ScheduleAdd(body, expire, timestamp, peer, replyTo) =>
          if(timerStorage.hasTimer(body.id)){
            Effect.unhandled.thenReply(replyTo)(_=>Reject)
          }else{
            Effect.persist(ScheduleAdded(body, expire, timestamp)).thenReply(replyTo)(_ => Success)
          }
        case ScheduleDel(id, peer, replyTo) =>
          if(timerStorage.hasTimer(id) || expireSchedules.contains(id)){
            Effect.persist(ScheduleDeleted(id)).thenReply(replyTo)(_ => Success)
          }else{
            Effect.none.thenReply(replyTo)(_ => Reject)
          }
        case ScheduleExpire(body) =>
          Effect.persist(ScheduleExpired(body)).thenNoReply()
          //触发到期事件
        case ScheduleDo(id) =>
          Effect.persist(ScheduleDone(id)).thenNoReply()
          //执行调度失败，重新启用
        case ScheduleRedo(body) =>
          //将执行中改为未执行
          expireSchedules(body.id).actioning = false
          Effect.none.thenNoReply()
          //定时执行调度
        case ScheduleAction =>
          val now = Deadline.now
          //将在执行中的，没有到需要执行时间的过滤掉
          expireSchedules.values.filter(schedule => schedule.actioning).filter(schedule => schedule.nextActionTimer < now ).foreach{schedule =>
            //当前时间加上执行间隔就是下一次要执行的时间
            schedule.nextActionTimer = now + option.scheduleActionInterval
            //执行定时任务
            val actorRef = context.spawnAnonymous(ScheduleBroker(context.self,schedule.body))
            actorRef ! Expire
          }
          Effect.none.thenNoReply()
      }
    }

    override def applyEvent(event: Event): Timer = {
      context.log.debug(s"活跃定时器接收到事件$event")
      event match {
        case ScheduleAdded( body, expire, timestamp) =>
          val currentTimestamp = System.currentTimeMillis()
          //计算当前时间和命令发送时间时间差，以减少误差
          val realExpire = expire - (currentTimestamp - timestamp)
          context.log.debug(s"命令发送时间：$timestamp,当前时间$currentTimestamp,原始偏移量：$expire,当前到期时间偏移量，$realExpire")
          timerStorage.save(ScheduleExecutor(body.id, if(realExpire > 0) realExpire else 0, body))
          this
        case ScheduleDeleted(id) =>
          context.log.debug(s"接收到删除调度任务命令 actionId：$id")
          timerStorage.delete(id)
          expireSchedules -= id
          this
        case ScheduleExpired(body) =>
          expireSchedules += (body.id -> ScheduleContext(body,Deadline.now))
          this
        case ScheduleDone(id) =>
          expireSchedules -= id
          this

      }
    }

    override def fetchEvent(): List[Event] = Nil
  }


}
