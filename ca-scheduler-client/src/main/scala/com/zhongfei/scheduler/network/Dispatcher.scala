package com.zhongfei.scheduler.network

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import com.zhongfei.scheduler.network.Dispatcher.{Command, ConnectionCompleted}
import com.zhongfei.scheduler.transport.protocol.SchedulerProtocol.Protocol
import com.zhongfei.scheduler.transport.{Node, Peer}

object Dispatcher{
  trait Command
  case class ConnectionCompleted(peer: Peer) extends Command
  case class ProtocolCommand(protocol: Protocol) extends Command
}

/**
 * 客户端分发器负责分发请求
 * @param node
 * @param context
 */
class Dispatcher(node:Node, context:ActorContext[Command]) {
  def process():Behavior[Command] = Behaviors.receiveMessage{ message =>
    message match {
      case ConnectionCompleted(peer) =>

    }

    Behaviors.same
  }
}
