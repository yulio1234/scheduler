package com.zhongfei.scheduler.network

import akka.actor.typed.scaladsl.{ActorContext, Behaviors, TimerScheduler}
import akka.actor.typed.{ActorRef, Behavior}
import com.zhongfei.scheduler.Exception.SchedulerExceptionFactory
import com.zhongfei.scheduler.network.Dispatcher._
import com.zhongfei.scheduler.transport.Peer
import com.zhongfei.scheduler.transport.protocol.ApplicationOption
import com.zhongfei.scheduler.utils.IDGenerator
import io.netty.channel.ChannelFuture

object Dispatcher {

  trait Message
  case class Timeout(actionId:Long) extends Message
  //命令（请求）
  //应用心跳检测请求
  case class HeartBeat(actionId: Long = IDGenerator.next(),applicationOption: ApplicationOption, peer: Peer, replyTo: ActorRef[HeartBeaten]) extends Message
  //心跳事件（响应）
  case class HeartBeaten(actionId: Long,success:Boolean,cause:Throwable,peer: Peer) extends Message

  //应用取消注册请求
  case class Unregister(actionId: Long, appName: String) extends Message
  //应用取消注册响应
  case class Unregistered(actionId: Long,success:Boolean,cause:Throwable,peer: Peer) extends Message
  case class ScheduleAdd(actionId:Long,body:ScheduleBody,expire:Long,peer: Peer,replyTo: ActorRef[ScheduleAdded]) extends Message
  case class ScheduleBody(id:Long,domain:String,eventName:String,extra:String)
  case class ScheduleAdded(actionId:Long,success:Boolean,cause:Throwable,peer: Peer) extends Message

  case class ActorTerminate(id: Long) extends Message

  def apply(option: ClientOption): Behavior[Message] = Behaviors.setup { context => Behaviors.withTimers{timer =>
    new Dispatcher(option,timer, context).process(Map.empty)
    }
  }

}

class Dispatcher(option: ClientOption,  timers: TimerScheduler[Message],context: ActorContext[Message]) {
  def process(map: Map[Long, Message]): Behavior[Message] = Behaviors.receiveMessage {
    //发送心跳请求
    case heartBeat: HeartBeat =>
      context.log.debug(s"收到心跳发送请求，发送心跳消息：$heartBeat")
      //接收请求后，定义超时检查
      timers.startSingleTimer("Timeout"+heartBeat.actionId,Timeout(heartBeat.actionId),option.transferTimeoutInterval)
      //发送请求
      heartBeat.peer.send(heartBeat).addListener((future: ChannelFuture) => {
        //通讯失败就返回
        if (!future.isSuccess) {
          val exception = SchedulerExceptionFactory.NetworkTransferException(cause = future.cause())
          context.self ! HeartBeaten(heartBeat.actionId,future.isSuccess,exception,heartBeat.peer)
        }
      })
      process(map + (heartBeat.actionId -> heartBeat))
    //接收心跳请求
    case heartBeaten: HeartBeaten =>
      context.log.debug(s"从服务器节点：${heartBeaten.peer},接收到心跳响应：$heartBeaten")
      context.log.debug(s"容器内的数据为：size=${map.size},key=${map.keys}")
      val message = map.get(heartBeaten.actionId)
      message match {
        case Some(value) =>
          if (value.isInstanceOf[HeartBeat]) {
            value.asInstanceOf[HeartBeat].replyTo ! heartBeaten
          }else{
            context.log.error(s"消息类型不匹配，保存的消息$value,返回的消息$heartBeaten")
          }
          process(map - heartBeaten.actionId)
        case None =>
          context.log.error(s"没有找到相应的Actor事件处理器 消息体：$heartBeaten")
          Behaviors.same
      }
      //发送调度添加请求
    case scheduleAdd: ScheduleAdd =>
      context.log.debug(s"收到调度器添加发送请求，发送调度消息：$scheduleAdd")
      //解析协议body

      //接收请求后，定义超时检查
      timers.startSingleTimer("Timeout"+scheduleAdd.actionId,Timeout(scheduleAdd.actionId),option.transferTimeoutInterval)
      //发送请求
      scheduleAdd.peer.send(scheduleAdd).addListener((future: ChannelFuture) => {
        //通讯失败就返回
        if (!future.isSuccess) {
          val exception = SchedulerExceptionFactory.NetworkTransferException(cause = future.cause())
          context.self ! ScheduleAdded(scheduleAdd.actionId,future.isSuccess,exception,scheduleAdd.peer)
        }
      })
      process(map + (scheduleAdd.actionId -> scheduleAdd))
      //响应
    case schedulerAdded:ScheduleAdded =>
      map.get(schedulerAdded.actionId) match {
        case Some(value) =>
          if (value.isInstanceOf[ScheduleAdd]) {
            value.asInstanceOf[ScheduleAdd].replyTo ! schedulerAdded
          }
        case None =>  context.log.error(s"没有找到相应的Actor事件处理器 消息体：$schedulerAdded")
      }
      process(map - schedulerAdded.actionId)
    case Timeout(actionId) =>
      context.log.warn("消息发送超时,actionId="+actionId)
      process(map - actionId)
  }
}
