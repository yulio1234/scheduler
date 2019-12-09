package com.zhongfei.scheduler.network

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, TimerScheduler}
import com.zhongfei.scheduler.network.SchedulerConnection.{Command, SendHeatBeat}
import com.zhongfei.scheduler.network.SchedulerConnectionManager.Register
import com.zhongfei.scheduler.transport.NettyTransfer.WrappedRequest
import com.zhongfei.scheduler.transport.{NettyTransfer, Peer}
import com.zhongfei.scheduler.transport.protocol.SchedulerProtocol.{ActionTypeEnum, Request}
import com.zhongfei.scheduler.utils.IDGenerator

import scala.concurrent.duration.{Deadline, FiniteDuration}

object SchedulerConnection{
  trait Command
  case object SendHeatBeat extends Command
  def apply(option: ClientOption,peer: Peer): Behavior[Command] = Behaviors.setup{context => Behaviors.withTimers{timers => new SchedulerConnection(option,peer,timers,context).handle(Deadline.now.time)}}
}

/**
 * 服务
 * @param option
 * @param peer
 * @param timers
 * @param context
 */
class SchedulerConnection(option: ClientOption, peer: Peer, timers: TimerScheduler[Command], context:ActorContext[Command]) {
  timers.startTimerAtFixedRate(SendHeatBeat,SendHeatBeat,option.heartBeatInterval)
  def handle(lastedHeartBeatTime:FiniteDuration):Behavior[Command] = Behaviors.receiveMessage{ message =>
    message match {
        //注册成功之后，向服务器发送心跳请求
      case  Register(peer)=>
        sendHeartBeat
        Behaviors.same
        //定时向服务器发送心跳请求
      case SendHeatBeat =>
        sendHeartBeat
        Behaviors.same

    }
  }
  def sendHeartBeat(): Unit ={
    val transfer = context.spawnAnonymous(NettyTransfer(peer.channel,option.transferRetryCount,option.heartBeatInterval))
    transfer ! WrappedRequest(Request(actionId = IDGenerator.next(),actionType = ActionTypeEnum.HeartBeat.id.toByte))
  }
}
