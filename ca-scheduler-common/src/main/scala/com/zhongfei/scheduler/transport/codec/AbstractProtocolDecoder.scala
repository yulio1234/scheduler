package com.zhongfei.scheduler.transport.codec

import akka.actor.typed.ActorRef
import com.zhongfei.scheduler.transport.Peer
import com.zhongfei.scheduler.transport.protocol.SchedulerProtocol.Protocol
import com.zhongfei.scheduler.utils.Logging

/**
 * 抽象解码处理器
 * @tparam P 可解析的协议消息，现阶段为Request和Response
 * @tparam U 返回的消息类型，用于给actor发送的actor内部子协议
 * @tparam C actor内部的内部协议父类，用于接收所有子协议
 */
abstract class AbstractProtocolDecoder[P<:Protocol,U <: C,C] extends Decoder[P,U] with Command[P,C] with Logging{

  /**
   * 将外部协议，转换为actor可以执行的子协议
   * @param protocol
   * @param actor
   * @param peer
   */
  override def execute(protocol: P, actor: ActorRef[C], peer: Peer): Unit = {
      val command = decode(protocol,peer)
      if(actor == null){
        throw new RuntimeException("actor 为空")
      }
    command match {
      case Some(value) => actor ! value
      case None => warn(s"decode调用没有返回值，peer：$peer")
    }


  }

  /**
   * 执行节码操作
   *
   * @param msg 需要节码的消息
   * @return 返回解码后的对象
   */
  override def decode(msg: P,peer: Peer):Option[U]
}
