package com.zhongfei.scheduler.network

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import com.zhongfei.scheduler.network.ApplicationManager.{HeartBeat, Unregister}
import com.zhongfei.scheduler.network.ServerDispatcher.{Command, WrappedHeartBeat, WrappedScheduleAdd, WrappedUnregister}
import com.zhongfei.scheduler.options.ServerOption
import com.zhongfei.scheduler.timer.TimerEntity
import com.zhongfei.scheduler.timer.TimerEntity.{ScheduleAdd, ScheduleDel}
import com.zhongfei.scheduler.transfer.{OperationResult, Transfer}
import com.zhongfei.scheduler.transport.Peer

/**
 * 调度处理器处理器
 */
object ServerDispatcher {

  sealed trait Command
  case class WrappedHeartBeat(heartBeat: HeartBeat) extends Command
  case class WrappedScheduleAdd(scheduleAdd: ScheduleAdd) extends Command
  case class WrappedScheduleDel(scheduleDel: ScheduleDel) extends Command
  case class WrappedUnregister(unregister: Unregister) extends Command

  def apply(option: ServerOption,system:ActorSystem[Nothing]): Behavior[Command] = Behaviors.setup{ context => new ServerDispatcher(option,context,system).process()}
}

/**
 * 全局消息处理器，负责分发各种消息
 * @param option
 */
private class ServerDispatcher(option:ServerOption, context:ActorContext[Command],system:ActorSystem[Nothing])  {
  //创建应用管理者
  val applicationManager = context.spawn(ApplicationManager(option),"applicationManager")
  // TODO:  进行查询数据保存
  def process(): Behavior[Command] = Behaviors.receiveMessage[Command]{message => {
    message match {
      case WrappedHeartBeat(heartBeat) =>
        val actorRef = context.spawnAnonymous(Transfer(option, heartBeat.peer, message))
        //转发给应用管理器
        applicationManager ! heartBeat.copy(replyTo = actorRef)
        Behaviors.same
        //匹配注销应用请求
      case WrappedUnregister(unregister) =>
        val actorRef = context.spawnAnonymous(Transfer(option, unregister.peer, message))
        applicationManager ! unregister.copy(replyTo = actorRef)
        Behaviors.same
      case WrappedScheduleAdd(scheduleAdd) =>
        val clusterSharding: ClusterSharding = ClusterSharding(system)
        val actorRef = context.spawnAnonymous(Transfer(option, scheduleAdd.peer, message))
        val timerRef = clusterSharding.entityRefFor(TimerEntity.timerTypeKey, scheduleAdd.body.appName)
        timerRef ! scheduleAdd.copy(replyTo = actorRef)
        Behaviors.same
    }
  }}
  def buildTransfer(command:Command,peer: Peer): ActorRef[OperationResult] ={
    context.spawnAnonymous(Transfer(option, peer, command))
  }
}
