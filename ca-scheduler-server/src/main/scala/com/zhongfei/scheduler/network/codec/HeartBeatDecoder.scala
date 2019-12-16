package com.zhongfei.scheduler.network.codec

import com.zhongfei.scheduler.command.SchedulerCommand.HeartBeat
import com.zhongfei.scheduler.network.ServerDispatcher.Command
import com.zhongfei.scheduler.transport.Peer
import com.zhongfei.scheduler.transport.codec.RequestProtocolDecoder
import com.zhongfei.scheduler.transport.protocol.SchedulerProtocol
import com.zhongfei.scheduler.utils.Logging

/**
 * 心跳解码器，处理心跳节码请求，并执行
 */
class HeartBeatDecoder extends RequestProtocolDecoder[HeartBeat,Command] with Logging{

  /**
   * 执行节码操作
   *
   * @param msg 需要节码的消息
   * @return 返回节码后的对象
   */
  override def decode(msg: SchedulerProtocol.Request,peer: Peer): Option[HeartBeat] = {
    if (msg.length.toInt > 0) {
      debug(s"处理心跳请求：${new String(msg.content)},对等端：${Peer}")
      Some(HeartBeat(msg.actionId,new String(msg.content),peer,null))
    }else{
      None
    }

  }
}
