package com.zhongfei.scheduler.network

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, TimerScheduler}
import com.zhongfei.scheduler.Exception.SchedulerExceptionFactory
import com.zhongfei.scheduler.network.Dispatcher._
import com.zhongfei.scheduler.transport.protocol.ScheduleActionProtocol
import com.zhongfei.scheduler.transport.protocol.ScheduleActionProtocol.OperationResult
import com.zhongfei.scheduler.transport.protocol.SchedulerProtocol.ActionTypeEnum
import io.netty.channel.ChannelFuture

object Dispatcher {

  trait Command
  case class Timeout(actionId:Long) extends Command
  //命令（请求）
  //心跳检测请求
  case class WrappedHeartBeat(heartBeat: ScheduleActionProtocol.HeartBeat) extends Command
  //取消注册请求
  case class WrappedUnregister(unregister: ScheduleActionProtocol.Unregister) extends Command
  //定时调度添加请求
  case class WrappedScheduleAdd(scheduleAdd:ScheduleActionProtocol.ScheduleAdd) extends Command
  //定时调度删除请求
  case class WrappedScheduleDel(scheduleDel:ScheduleActionProtocol.ScheduleDel) extends Command
  case class WrappedOperationResult(operationResult: ScheduleActionProtocol.OperationResult) extends Command with SchedulerConnection.Command
  case class ActorTerminate(id: Long) extends Command

  def apply(option: ClientOption): Behavior[Command] = Behaviors.setup { context => Behaviors.withTimers{timer =>
    new Dispatcher(option,timer, context).process(Map.empty)
    }
  }

}

class Dispatcher(option: ClientOption,  timers: TimerScheduler[Command],context: ActorContext[Command]) {
  def process(map: Map[Long, Command]): Behavior[Command] = Behaviors.receiveMessage {
    //发送心跳请求
    case command @ WrappedHeartBeat(heartBeat) =>
      context.log.debug(s"收到心跳发送请求，发送心跳消息：$heartBeat")
      //接收请求后，定义超时检查
      timers.startSingleTimer("Timeout"+heartBeat.actionId,Timeout(heartBeat.actionId),option.transferTimeoutInterval)
      //发送请求
      heartBeat.peer.send(heartBeat).addListener((future: ChannelFuture) => {
        //通讯失败就返回
        if (!future.isSuccess) {
          val exception = SchedulerExceptionFactory.NetworkTransferException(cause = future.cause())
          context.self ! WrappedOperationResult(OperationResult(heartBeat.actionId,ActionTypeEnum.HeartBeat,false,exception))
        }
      })
      process(map + (heartBeat.actionId -> command))
      //发送调度添加请求
    case command @ WrappedScheduleAdd(scheduleAdd) =>
      context.log.debug(s"收到调度器添加发送请求，发送调度消息：$scheduleAdd")
      //解析协议body

      //接收请求后，定义超时检查
      timers.startSingleTimer("Timeout"+scheduleAdd.actionId,Timeout(scheduleAdd.actionId),option.transferTimeoutInterval)
      //发送请求
      scheduleAdd.peer.send(scheduleAdd).addListener((future: ChannelFuture) => {
        //通讯失败就返回
        if (!future.isSuccess) {
          val exception = SchedulerExceptionFactory.NetworkTransferException(cause = future.cause())
          context.self ! WrappedOperationResult(OperationResult(scheduleAdd.actionId,ActionTypeEnum.ScheduleAdd,false,exception))
        }
      })
      process(map + (scheduleAdd.actionId -> command))
    //接收到响应
    case WrappedOperationResult(operationResult) =>

      context.log.debug(s"从服务器节点接收到响应：$operationResult")
      context.log.debug(s"容器内的数据为：size=${map.size},key=${map.keys}")
      val message = map.get(operationResult.actionId)
      message match {
        case Some(command:WrappedScheduleAdd) =>
          command.scheduleAdd.replyTo ! operationResult
          process(map - command.scheduleAdd.actionId)
        case Some(command:WrappedScheduleDel) =>
          command.scheduleDel.replyTo ! operationResult
          process(map - command.scheduleDel.actionId)
        case None =>
          context.log.error(s"没有找到相应的Actor事件处理器 消息体：$operationResult")
          Behaviors.same
      }
    case Timeout(actionId) =>
      context.log.warn("消息发送超时,actionId="+actionId)
      process(map - actionId)
  }
}
