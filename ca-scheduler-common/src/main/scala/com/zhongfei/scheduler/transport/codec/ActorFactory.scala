package com.zhongfei.scheduler.transport.codec

import akka.actor.typed.ActorRef

/**
 * actor工厂
 * @tparam T
 */
trait ActorFactory[T] {
  def create:ActorRef[T]
}
