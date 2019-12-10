package com.zhongfei.scheduler.transport.codec

import akka.actor.typed.ActorRef
import com.zhongfei.scheduler.transport.Peer

/**
 * 命令模式：
 * 可执行的命令对象抽象
 * @tparam P 可解析的协议消息，现阶段可解析为Request和Response
 * @tparam C actor内部的内部协议父类，用于接收所有子协议
 */
trait Command[P,C] {
  def execute(obj:P,actor:ActorRef[C],peer:Peer):Unit
}
