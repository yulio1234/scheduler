package com.zhongfei.scheduler.transport.codec

import akka.actor.typed.ActorRef
import com.zhongfei.scheduler.transport.Peer

/**
 * 命令模式：
 * 可执行的命令对象抽象
 * @tparam P 可执行的消息对象
 * @tparam C actor可执行的命令对象
 */
trait Command[P,C] {
  def execute(obj:P,actor:ActorRef[C],peer:Peer):Unit
}
