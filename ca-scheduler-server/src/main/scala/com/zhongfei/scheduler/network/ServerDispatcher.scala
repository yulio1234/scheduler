package com.zhongfei.scheduler.network

import akka.actor.typed.scaladsl.{ActorContext, Behaviors, TimerScheduler}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import com.zhongfei.scheduler.network.Application.{ScheduleExpire, ScheduleExpired}
import com.zhongfei.scheduler.network.ServerDispatcher._
import com.zhongfei.scheduler.options.ServerOption
import com.zhongfei.scheduler.timer.TimerEntity
import com.zhongfei.scheduler.transfer.Transfer
import com.zhongfei.scheduler.transport.Peer
import com.zhongfei.scheduler.transport.protocol.ScheduleActionProtocol._
import com.zhongfei.scheduler.utils.IDGenerator
/**
 * 调度处理器处理器
 */
object ServerDispatcher {

  trait Command

  case class WrappedHeartBeat(heartBeat: HeartBeat) extends Command with Transfer.Command

  case class WrappedScheduleAdd(scheduleAdd: ScheduleAdd) extends Command with Transfer.Command

  case class WrappedScheduleDel( scheduleDel: ScheduleDel) extends Command with Transfer.Command

  case class WrappedUnregister(unregister: Unregister) extends Command with Transfer.Command

  case class WrappedOperationResult(operationResult: OperationResult) extends Command

  case class WrappedScheduleExpire(actionId: Long = IDGenerator.next(), peer: Peer, scheduleExpire: ScheduleExpire) extends Command with Transfer.Command

  case class WrappedScheduleExpired(actionId: Long, peer: Peer, scheduleExpired: ScheduleExpired) extends Command with Transfer.Command

  case class Timeout(actionId:Long) extends Command

  def apply(option: ServerOption, system: ActorSystem[Nothing]): Behavior[Command] = Behaviors.setup { context =>
    Behaviors.withTimers{timers=>
    new ServerDispatcher(option,timers, context, system).process(Map.empty)
    }
  }

}

/**
 * 全局消息处理器，负责分发各种消息
 *
 * @param option
 */
private class ServerDispatcher(option: ServerOption,timers:TimerScheduler[Command], context: ActorContext[Command], system: ActorSystem[Nothing]) {
  //创建应用管理者
  val applicationManager = context.spawn(ApplicationManager(option, context.self), "applicationManager")

  def process(map: Map[Long, Command]): Behavior[Command] = Behaviors.receiveMessage[Command] {
      //接收定时器到期消息
    case command: WrappedScheduleExpire =>
      timers.startSingleTimer(command.actionId,Timeout(command.actionId),command.scheduleExpire.processWaitTimer)
      val transfer = context.spawnAnonymous(Transfer(option, command.peer, command))
      transfer ! command
      process(map + (command.actionId -> command))
      //接收定时器到期回复
    case command: WrappedScheduleExpired =>
      timers.cancel(command.actionId)
      map(command.actionId) match {
        case WrappedScheduleExpire(_, _, scheduleExpire) =>
          scheduleExpire.replyTo ! command.scheduleExpired
      }
      process(map - command.actionId)
    case command@WrappedHeartBeat(heartBeat) =>
      val actorRef = context.spawnAnonymous(Transfer(option, heartBeat.peer, command))
      //转发给应用管理器
      applicationManager ! heartBeat.copy(replyTo = actorRef)
      Behaviors.same
    //匹配注销应用请求
    case command @ WrappedUnregister(unregister) =>
      val transfer = context.spawnAnonymous(Transfer(option, unregister.peer, command))
      transfer ! command
      applicationManager ! unregister.copy(replyTo = transfer)
      Behaviors.same
    case command @ WrappedScheduleAdd(scheduleAdd) =>
      val clusterSharding: ClusterSharding = ClusterSharding(system)
      val transfer = context.spawnAnonymous(Transfer(option, scheduleAdd.peer, command))
      val timerRef = clusterSharding.entityRefFor(TimerEntity.timerTypeKey, scheduleAdd.body.domain)
      timerRef ! scheduleAdd.copy(replyTo = transfer)
      Behaviors.same
      //请求超时
    case Timeout(actionId) =>
      process(map -actionId)
  }

  def buildTransfer(command: Command, peer: Peer): ActorRef[OperationResult] = {
    context.spawnAnonymous(Transfer(option, peer, command))
  }

}
