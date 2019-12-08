package com.zhongfei.scheduler.network

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import com.zhongfei.scheduler.command.SchedulerCommand
import com.zhongfei.scheduler.network.CoreDispatcher.{Command, ProtocolCommand}
import com.zhongfei.scheduler.options.SingletonOption
import com.zhongfei.scheduler.transport.Peer
import com.zhongfei.scheduler.transport.protocol.SchedulerProtocol.Protocol

/**
 * 调度处理器处理器
 */
object CoreDispatcher {

  trait Command extends SchedulerCommand.Command
  case class AppManagerCmdTimeout()

  //与应用通讯的消息的消息
  final case class ProtocolCommand(peer: Peer, protocol: Protocol) extends Command with ApplicationDispatcher.Command

  def apply(option: SingletonOption): Behavior[Command] = Behaviors.setup{ context => new CoreDispatcher(option,context).process()}
}

/**
 * 全局消息处理器，负责分发各种消息
 * @param option
 */
private class CoreDispatcher(option:SingletonOption, context:ActorContext[Command])  {
  //创建应用管理者
  val applicationManager = context.spawn(ApplicationManager(option,context.self),"applicationManager")

  // TODO:  进行查询数据保存
  def process(): Behavior[Command] = Behaviors.receiveMessage[Command]{message => {
    message match {
      case command: ProtocolCommand =>
        //创建应用处理器
        context.log.info("接收到命令请求")
        val applicationProcessor = context.spawnAnonymous(ApplicationDispatcher(option,command.peer,context.self,applicationManager))
        applicationProcessor ! command
        Behaviors.same
    }
  }}
}
