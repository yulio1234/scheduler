package com.zhongfei.scheduler.network

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import com.zhongfei.scheduler.Exception.SchedulerExceptionFactory
import com.zhongfei.scheduler.network.ServerDispatcher.ScheduleAdd
import com.zhongfei.scheduler.network.TimerDispatcher.{Command, WrappedOperationResult}
import com.zhongfei.scheduler.timer.TimerEntity
import com.zhongfei.scheduler.timer.TimerEntity.ScheduleAddBody
import com.zhongfei.scheduler.transport.Peer
import com.zhongfei.scheduler.transport.protocol.SchedulerProtocol.Response
import io.netty.channel.ChannelFuture
object TimerDispatcher{
  trait Command

  case class ActorTerminate(id: Long) extends Command


  final case class WrappedOperationResult(operationResult: TimerEntity.OperationResult) extends Command
}
/**
 * 定时器分发器，处理定时任务消息
 */
class TimerDispatcher(peer: Peer,context: ActorContext[Command],system:ActorSystem[Nothing]) {
  private val clusterSharding: ClusterSharding = ClusterSharding(system)

  private val operationResultAdapter: ActorRef[TimerEntity.OperationResult] = context.messageAdapter(WrappedOperationResult)
  def handler(): Behavior[Command] = Behaviors.receiveMessage{
    case ScheduleAdd(actionId,body,expire,timestamp,peer,replyTo) =>
      val appName = body.appName
      val timerRef = clusterSharding.entityRefFor(TimerEntity.timerTypeKey, appName)
      timerRef ! TimerEntity.ScheduleAdd(actionId,ScheduleAddBody(body.appName,body.eventName,body.extra),expire,timestamp,operationResultAdapter)
      Behaviors.same
      //如果成功就想远端回复成功
    case WrappedOperationResult(operationResult) =>
      val response = Response(actionId = operationResult.actionId)
      peer.channel.writeAndFlush(response).addListener((future: ChannelFuture) => {
        //通讯失败就返回
        if (!future.isSuccess) {
          val exception = SchedulerExceptionFactory.NetworkTransferException(cause = future.cause())
          context.self ! HeartBeaten(heartBeat.actionId,future.isSuccess,exception,heartBeat.peer)
        }
      })
      Behaviors.same
  }
}
