package com.zhongfei.scheduler.processor

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import com.zhongfei.scheduler.convert.AppManagerCmdConverter
import com.zhongfei.scheduler.options.DispatchProcessorOption
import com.zhongfei.scheduler.processor.DispatchProcessor.{Command, RequestCommand}
import com.zhongfei.scheduler.transport.Peer
import com.zhongfei.scheduler.transport.protocol.SchedulerProtocol.Protocol
import com.zhongfei.scheduler.{Message, Processor}

/**
 * 调度处理器处理器
 */
object DispatchProcessor {

  trait Command extends Message
  case
  case class AppManagerCmdTimeout()

  //与应用通讯的消息的消息
  case class RequestCommand(peer: Peer, protocol: Protocol) extends Command

  def apply(option: DispatchProcessorOption): Behavior[Command] = Behaviors.setup{ context => new DispatchProcessor(option,context).process(???)}
}

/**
 * 全局消息处理器，负责分发各种消息
 * @param option
 * @param actorContext
 */
private class DispatchProcessor(option:DispatchProcessorOption,actorContext:  ActorContext[Command]) extends Processor[Nothing,Behavior[Command]] {


  override def process(none:Nothing): Behavior[Command] = Behaviors.receiveMessage[Command]{message => {
    message match {
      case request: RequestCommand => onRequestCommand(request)
    }
  }}


  /**
   * 处理应用消息
   *
   * @param request
   */
  def onRequestCommand(request: RequestCommand): Behavior[Command] = {
    val peer = request.peer
    val protocol = request.protocol
    val command = AppManagerCmdConverter.convert(protocol, peer)
    val actor = option.appManagerActor
    actor ! command
    Behaviors.same
  }
}
