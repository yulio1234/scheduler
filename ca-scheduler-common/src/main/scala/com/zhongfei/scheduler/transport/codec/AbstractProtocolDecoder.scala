package com.zhongfei.scheduler.transport.codec

import akka.actor.typed.ActorRef
import com.zhongfei.scheduler.transport.Peer
import com.zhongfei.scheduler.transport.protocol.SchedulerProtocol.Protocol

/**
 * 抽象编码处理器
 * @tparam P 可执行的消息对象
 * @tparam U 返回的消息类型
 * @tparam C actor可执行的命令对象
 */
abstract class AbstractProtocolDecoder[P <: Protocol,U <: C,C] extends Decoder[P,U] with Command[P,C] {
  def execute(protocol: P,actor:ActorRef[C],peer:Peer): Unit ={
    val command = decode(protocol,peer)
    if(actor == null){
      throw new RuntimeException("actor 为空")
    }
    actor ! command
  }
  /**
   * 执行节码操作
   *
   * @param msg 需要节码的消息
   * @return 返回节码后的对象
   */
  override def decode(msg: P,peer: Peer):U
}
