package com.zhongfei.scheduler.network

import akka.actor.typed.scaladsl.{ActorContext, Behaviors, TimerScheduler}
import akka.actor.typed.{ActorRef, Behavior}
import com.zhongfei.scheduler.network.Dispatcher.{ActorTerminate, Done, HeartBeat, HeartBeaten, Message, Retry, Timeout}
import com.zhongfei.scheduler.transport.NettyTransfer.WrappedRequest
import com.zhongfei.scheduler.transport.protocol.SchedulerProtocol.{ActionTypeEnum, Protocol, Request, Response}
import com.zhongfei.scheduler.transport.{NettyTransfer, Peer}
import com.zhongfei.scheduler.utils.IDGenerator
import io.netty.channel.ChannelFuture

object Dispatcher {

  trait Message


  case class Done(id:Long,success: Boolean, retryCount: Int, cause: Throwable, message: Protocol,peer: Peer) extends Message

  case class Retry(done: Done) extends Message

  case class Timeout(actionId:Long) extends Message


  //命令（请求）
  //应用心跳检测请求
  case class HeartBeat(actionId: Long = IDGenerator.next(), appName: String, peer: Peer, replyTo: ActorRef[Message]) extends Message

  //应用取消注册请求
  case class Unregister(actionId: Long, appName: String)


  //事件（响应）
  case class HeartBeaten(actionId: Long) extends Message

  //应用取消注册成功
  case class Unregistered(actionId: Long) extends Message

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
      val bytes = heartBeat.appName.getBytes()
      val request = Request(actionId = IDGenerator.next(), actionType = ActionTypeEnum.HeartBeat.id.toByte, length = bytes.length.toShort, content = bytes)
      //接收请求后，定义超时检查
//      timers.startSingleTimer("Timeout"+heartBeat.actionId,Timeout(heartBeat.actionId),option.transferTimeoutInterval)
      //发送请求
      send(heartBeat.peer,option.transferRetryCount,request,context.self)
      process(map + (heartBeat.actionId -> heartBeat))
    //接收心跳请求
    case heartBeaten: HeartBeaten =>
      context.log.debug(s"接收到心跳响应：$heartBeaten")
      val message = map.get(heartBeaten.actionId)
      message match {
        case Some(value) =>
          if (value.isInstanceOf[HeartBeat]) {
            value.asInstanceOf[HeartBeat].replyTo ! heartBeaten
          }else{
            context.log.error("消息类型不匹配")
          }
          process(map - heartBeaten.actionId)
        case None =>
          context.log.error(s"没有找到相应的Actor事件处理器 消息体：$heartBeaten")
          Behaviors.same
      }
    case done: Done if done.success =>
      context.log.debug("响应消息发送成功，清除消息")
      process(map - done.message.actionId)
    case done: Done if done.success =>
      //如果重试次数大于0，就重试
      if (done.retryCount > 0) {
        timers.startSingleTimer("Retry"+done.message.actionId,Retry(done.copy(retryCount = done.retryCount -1)),option.transferRetryInterval)
        Behaviors.same
      } else {
        //重试次数耗尽
        context.log.error(s"重试次数耗尽，响应消息发送失败,消息：${done.message}", done.cause)
        //次数耗尽后关闭超时检查
        timers.cancel("Timeout"+done.message.actionId)
        process(map - done.message.actionId)
      }
    case retry: Retry =>
      context.log.error(s"响应消息发送失败,进行重试，剩余次数：${retry.done.retryCount},消息体：${retry.done.message}")
      send(retry.done.peer,retry.done.retryCount, retry.done.message, context.self)
      Behaviors.same
    case Timeout(actionId) =>
      context.log.warn("消息发送超时,actionId="+actionId)
      //超时后，关闭重试检查
      timers.cancel("Retry"+actionId)
      process(map - actionId)
  }

  def send(peer: Peer, retryCount: Int, protocol: Protocol, actorRef: ActorRef[Message]): Unit = {
    peer.channel.writeAndFlush(protocol).addListener((future: ChannelFuture) => {
//      actorRef ! Done(future.isSuccess, retryCount, future.cause(), protocol,peer)
    })
  }
}
