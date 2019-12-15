package com.zhongfei.scheduler.transport.codec

import akka.actor.typed.ActorRef

/**
 * 协议处理器工厂
 */
abstract class ProtocolHandlerFactory[C,T] {
  def create(actor:ActorRef[C]): T
}
