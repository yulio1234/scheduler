package com.zhongfei.scheduler.network

import akka.actor.typed.scaladsl.{ActorContext, Behaviors, TimerScheduler}
import akka.actor.typed.{ActorRef, Behavior}
import com.zhongfei.scheduler.network.Application.{ChannelClose, CheckHeatBeatTime, Command, ScheduleExpire}
import com.zhongfei.scheduler.network.ApplicationManager._
import com.zhongfei.scheduler.network.ServerDispatcher.WrappedScheduleExpire
import com.zhongfei.scheduler.options.ServerOption
import com.zhongfei.scheduler.timer.TimerEntity.ScheduleBody
import com.zhongfei.scheduler.transfer.OperationResult
import com.zhongfei.scheduler.transport.Peer
import com.zhongfei.scheduler.transport.protocol.ApplicationOption
import io.netty.channel.ChannelFuture

import scala.concurrent.duration._
object Application{
  trait Command
  case class ScheduleExpire(scheduleBody: ScheduleBody,processWaitTimer:FiniteDuration,replyTo:ActorRef[OperationResult]) extends Command
  case class ScheduleExpired(success:Boolean) extends OperationResult
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

  private def handle(lastedHeartBeatTime:FiniteDuration,applicationOption: ApplicationOption): Behavior[Application.Command] = Behaviors.receiveMessage{

        //应用查询返回
      case SelectAnApplication(_, replyTo) =>
        replyTo ! Some(ApplicationRef(applicationOption.processWaitTime,context.self))
        Behaviors.same
        //执行到期任务
      case  scheduleExpire: ScheduleExpire =>
        dispatcher ! WrappedScheduleExpire(peer = peer,scheduleExpire=scheduleExpire)
        Behaviors.same
        //接收并处理心跳请求,单机的不用回复地址
      case HeartBeat(applicationOption,replyTo) =>
        replyTo ! HeartBeaten
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

}
