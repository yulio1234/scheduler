package com.zhongfei.scheduler.network

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import com.zhongfei.scheduler.network.Dispatcher.{ActorTerminate, HeartBeat, HeartBeaten, Message}
import com.zhongfei.scheduler.transport.NettyTransfer.WrappedRequest
import com.zhongfei.scheduler.transport.protocol.SchedulerProtocol.{ActionTypeEnum, Request}
import com.zhongfei.scheduler.transport.{NettyTransfer, Peer}
import com.zhongfei.scheduler.utils.IDGenerator

object Dispatcher{
  trait Message
  //命令（请求）
  //应用心跳检测请求
  case class HeartBeat(actionId:Long = IDGenerator.next(),appName:String,peer: Peer,replyTo:ActorRef[Message]) extends Message

  //应用取消注册请求
  case class Unregister(actionId:Long,appName:String)


  //事件（响应）
  case class HeartBeaten(actionId:Long) extends Message
  //应用取消注册成功
  case class Unregistered(actionId:Long) extends Message

  case class ActorTerminate(id:Long) extends Message

  def apply(option: ClientOption): Behavior[Message] = Behaviors.setup{ context =>
    new Dispatcher(option, context).process(Map.empty)
  }

}
class Dispatcher(option: ClientOption,context:ActorContext[Message]) {
  def process(map: Map[Long,ActorRef[Message]]):Behavior[Message] = Behaviors.receiveMessage{
        //发送心跳请求
    case heartBeat: HeartBeat =>
      val transfer = context.spawnAnonymous(NettyTransfer(heartBeat.peer.channel, option.transferRetryCount, option.transferRetryInterval))
      val bytes = heartBeat.appName.getBytes()
      transfer ! WrappedRequest(Request(actionId = IDGenerator.next(), actionType = ActionTypeEnum.HeartBeat.id.toByte, length = bytes.length.toShort, content = bytes))
      context.watchWith(heartBeat.replyTo,ActorTerminate(heartBeat.actionId))
      process(map + (heartBeat.actionId -> heartBeat.replyTo))
      //接收心跳请求
    case heartBeaten: HeartBeaten =>
      context.log.debug(s"接收到心跳响应：$HeartBeaten")
      val actor = map.get(heartBeaten.actionId)
      actor match {
        case Some(value) =>
          value ! heartBeaten
          process(map - heartBeaten.actionId)
        case None =>
          context.log.error("没有找到相应的Actor事件处理器",heartBeaten)
          Behaviors.same
      }
      //如果actor关闭，就取消
    case ActorTerminate(actionId) =>
      process(map - actionId)

    Behaviors.same
  }
}
