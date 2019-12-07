package com.zhongfei.scheduler.processor

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import com.zhongfei.scheduler.convert.AppManagerCmdConverter
import com.zhongfei.scheduler.options.{GlobalProcessorOption, StartModel}
import com.zhongfei.scheduler.processor.GlobalProcessor.{AppMessage, Command}
import com.zhongfei.scheduler.transport.Peer
import com.zhongfei.scheduler.transport.protocol.SchedulerProtocol.Protocol
import com.zhongfei.scheduler.{Message, Processor}

/**
 * 全局处理器
 */
object GlobalProcessor{
  trait Command extends Message
  case class AppManagerCmdTimeout()
  //与应用通讯的消息的消息
  case class AppMessage(peer: Peer,protocol: Protocol) extends Command
  def apply(option: GlobalProcessorOption): Behavior[Command] = Behaviors.setup(context =>
    option.startModel match {
      case n if (n ==StartModel.SINGLETON.id) => new GlobalProcessor(option,context).singletonProcess()
      case n if (n==StartModel.CLUSTER.id) =>new GlobalProcessor(option,context).clusterProcess()
    }
  )
}

/**
 * 全局消息处理器，集群单例对象，处理所有消息
 * @param option
 * @param actorContext
 */
private class GlobalProcessor(option:GlobalProcessorOption,actorContext:  ActorContext[Command]) extends Processor[Behavior[Command]] {


  override def process: Behavior[Command] = {
    option.startModel match {
      case n if(StartModel.SINGLETON == n) => singletonProcess()
      case n if(StartModel.CLUSTER == n) => clusterProcess()
    }
  }

  //单实例处理器
  private def singletonProcess():Behavior[Command]  = Behaviors.receiveMessage[Command]{message => {
    message match {
      case message: AppMessage => onAppMessage(message)
    }
  }}
  //集群处理器
  private def clusterProcess():Behavior[Command] = Behaviors.receiveMessage[Command]{ message => {

    Behaviors.same
  }}

  /**
   * 处理应用消息
   *
   * @param message
   */
  def onAppMessage(message: AppMessage): Behavior[Command] = {
    val peer = message.peer
    val protocol = message.protocol
    val command = AppManagerCmdConverter.convert(protocol, peer)
    val actor = option.appManagerActor
    actor ! command
    Behaviors.same
  }
}
