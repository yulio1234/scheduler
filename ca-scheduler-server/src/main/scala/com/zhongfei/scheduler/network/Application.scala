package com.zhongfei.scheduler.network

import akka.actor.typed.scaladsl.{ActorContext, Behaviors, TimerScheduler}
import akka.actor.typed.{ActorRef, Behavior}
import com.zhongfei.scheduler.command.SchedulerCommand.{HeartBeat, Unregister, Unregistered}
import com.zhongfei.scheduler.network.Application.{ChannelClose, CheckHeatBeatTime, Command}
import com.zhongfei.scheduler.options.SingletonOption
import com.zhongfei.scheduler.transport.NettyTransfer.WrappedResponse
import com.zhongfei.scheduler.transport.{NettyTransfer, Peer}
import com.zhongfei.scheduler.transport.protocol.SchedulerProtocol.Response
import io.netty.channel.ChannelFuture

import scala.concurrent.duration._
object Application{
  trait Command
  case object CheckHeatBeatTime extends Command
  case object ChannelClose extends Command
  trait Event
  def apply(option:SingletonOption,peer: Peer): Behavior[Command] = Behaviors.setup{context => Behaviors.withTimers{timers => new Application(option,peer,timers,context).handle(null)}}
}

/**
 * 应用处理器，专门处理应用相关消息
 * @param peer 对等体，代表通信的另一端
 * @param timers 调度器
 * @param context actor上下文
 */
private class Application(option:SingletonOption,peer: Peer, timers: TimerScheduler[Command], context:ActorContext[Application.Command]){
  timers.startTimerAtFixedRate(CheckHeatBeatTime,CheckHeatBeatTime,option.checkHeartbeatInterval)
  private val self: ActorRef[Command] = context.self
  //
  peer.channel.closeFuture().addListener((future: ChannelFuture) => {
      self ! ChannelClose
  })

  private def handle(lastedHeartBeatTime:FiniteDuration): Behavior[Application.Command] = Behaviors.receiveMessage{message =>{
    message match {
        //接收并处理心跳请求,单机的
      case HeartBeat(actionId, appName, peer) =>
        //发送心跳回复
        send(WrappedResponse(Response(actionId = actionId)))
        handle(Deadline.now.time)
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
  def send(wrappedResponse: WrappedResponse): Unit ={
    val transfer : ActorRef[NettyTransfer.Command] = context.spawn(NettyTransfer(peer.channel, option.transferRetryCount, option.transferRetryInterval), s"application-${peer.uri()}-transfer")
    transfer ! wrappedResponse
  }
}
