package com.zhongfei.scheduler.network

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, TimerScheduler}
import com.zhongfei.scheduler.network.SchedulerConnection.{CheckTimout, Command, HeartBeaten, Message, SendHeatBeat}
import com.zhongfei.scheduler.network.SchedulerConnectionManager.{ActiveServer, Event, Register}
import com.zhongfei.scheduler.transport.NettyTransfer.WrappedRequest
import com.zhongfei.scheduler.transport.{NettyTransfer, Node, Peer}
import com.zhongfei.scheduler.transport.protocol.SchedulerProtocol.{ActionTypeEnum, Request}
import com.zhongfei.scheduler.utils.IDGenerator

import scala.concurrent.duration.{Deadline, FiniteDuration}

object SchedulerConnection {

  trait Message

  trait Command extends Message

  trait Event extends Message

  //事件（响应）
  case class HeartBeaten(actionId: Long, hosts: Option[String]) extends Event

  case object SendHeatBeat extends Command

  case object CheckTimout extends Command

  def apply(option: ClientOption, node: Node, manager: ActorRef[SchedulerConnectionManager.Message]): Behavior[Message] =
    Behaviors.setup { context =>
      Behaviors.withTimers { timers =>
        new SchedulerConnection(option, node, timers, manager, context).handle(Deadline.now.time)
      }
    }
}

/**
 * 服务
 *
 * @param option
 * @param node
 * @param timers
 * @param context
 */
class SchedulerConnection(option: ClientOption, node: Node, timers: TimerScheduler[Command], manager: ActorRef[SchedulerConnectionManager.Message], context: ActorContext[Message]) {
  timers.startTimerAtFixedRate(SendHeatBeat, SendHeatBeat, option.sendHeartBeatInterval)

  def handle(lastedHeartBeatTime: FiniteDuration): Behavior[Message] = Behaviors.receiveMessage { message =>
    message match {
      //注册成功之后，向服务器发送心跳请求
      case Register(peer) =>
        sendHeartBeat
        Behaviors.same
      //定时向服务器发送心跳请求
      case SendHeatBeat =>
        sendHeartBeat
        Behaviors.same
      //收到心跳请求后，处理
      case HeartBeaten(actionId, hosts: Option[String]) =>
        manager ! ActiveServer(hosts)
        handle(Deadline.now.time)
      //检查超时时间
      case CheckTimout =>
        //当前时间减去最后一次记录的时间如果大于时间间隔就关闭
        val interval = Deadline.now - lastedHeartBeatTime
        if (interval.time > option.checkHeartBeatOnCloseInterval) {
          Behaviors.stopped
        } else {
          Behaviors.same
        }
    }
  }

  def sendHeartBeat(): Unit = {
    //    val transfer = context.spawnAnonymous(NettyTransfer(peer.channel,option.transferRetryCount,option.transferRetryInterval))
    //    transfer ! WrappedRequest(Request(actionId = IDGenerator.next(),actionType = ActionTypeEnum.HeartBeat.id.toByte))
  }
}
