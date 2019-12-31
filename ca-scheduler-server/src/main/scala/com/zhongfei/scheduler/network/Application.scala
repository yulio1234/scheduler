package com.zhongfei.scheduler.network

import akka.actor.typed.scaladsl.{ActorContext, Behaviors, TimerScheduler}
import akka.actor.typed.{ActorRef, Behavior}
import com.zhongfei.scheduler.network.Application.{ChannelClose, CheckHeatBeatTime, Command}
import com.zhongfei.scheduler.network.ApplicationManager.{ApplicationRef, HeartBeat, HeartBeaten, SelectAnApplication, Unregister, Unregistered}
import com.zhongfei.scheduler.network.ServerDispatcher.WrappedScheduleExpire
import com.zhongfei.scheduler.options.ServerOption
import com.zhongfei.scheduler.timer.TimerEntity.{ScheduleBody, ScheduleExpire}
import com.zhongfei.scheduler.transfer.OperationResult
import com.zhongfei.scheduler.transport.Peer
import com.zhongfei.scheduler.transport.protocol.ApplicationOption
import io.netty.channel.ChannelFuture

import scala.concurrent.duration._
object Application{
  trait Command
  case class ScheduleExpire(scheduleBody: ScheduleBody,replyTo:ActorRef[OperationResult])
  case object Success extends OperationResult
  case object Failure extends OperationResult
  case object CheckHeatBeatTime extends Command
  case object ChannelClose extends Command
  def apply(option:ServerOption,applicationOption: ApplicationOption,peer: Peer,dispatcher : ActorRef[WrappedScheduleExpire]): Behavior[Command] = Behaviors.setup{context => Behaviors.withTimers{timers => new Application(option,dispatcher,peer,timers,context).handle(Deadline.now.time,applicationOption)}}
}

/**
 * 应用处理器，专门处理应用相关消息
 * @param peer 对等体，代表通信的另一端
 * @param timers 调度器
 * @param context actor上下文
 */
private class Application(option:ServerOption,dispatcher : ActorRef[WrappedScheduleExpire],peer: Peer, timers: TimerScheduler[Command], context:ActorContext[Application.Command]){
  timers.startTimerAtFixedRate(CheckHeatBeatTime,CheckHeatBeatTime,option.checkHeartbeatInterval)
  private val self: ActorRef[Command] = context.self
  //
  peer.channel.closeFuture().addListener((future: ChannelFuture) => {
    if (future.isSuccess) {
      self ! ChannelClose
    }
  })

  private def handle(lastedHeartBeatTime:FiniteDuration,applicationOption: ApplicationOption): Behavior[Application.Command] = Behaviors.receiveMessage{ message =>{
    message match {
        //应用查询返回
      case SelectAnApplication(appName, replyTo) =>
        replyTo ! Some(ApplicationRef(applicationOption.processWaitTime,context.self))
        Behaviors.same
      case  scheduleExpire: ScheduleExpire =>
        dispatcher ! WrappedScheduleExpire(scheduleExpire,peer)
        Behaviors.same
        //接收并处理心跳请求,单机的不用回复地址
      case HeartBeat(actionId, applicationOption, _,replyTo) =>
        replyTo ! HeartBeaten(actionId)
        handle(Deadline.now.time,applicationOption)
        //如果是注销请求，就关闭应用
      case Unregister(actionId, _, _,reply) =>
        //回复消息
        reply ! Unregistered(actionId)
        Behaviors.stopped
        //处理心跳检测请求
      case CheckHeatBeatTime =>
        //当前时间减去最后一次记录的时间如果大于时间间隔就关闭
        val interval = Deadline.now - lastedHeartBeatTime
        if(interval.time > option.checkHeartBeatOnCloseInterval){
          Behaviors.stopped
        }else {
          Behaviors.same
        }
      case ChannelClose => Behaviors.stopped
    }
  }}
}
