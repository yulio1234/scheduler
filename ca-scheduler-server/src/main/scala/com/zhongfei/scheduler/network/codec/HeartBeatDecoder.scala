package com.zhongfei.scheduler.network.codec

import com.zhongfei.scheduler.command.SchedulerCommand.HeartBeat
import com.zhongfei.scheduler.network.CoreDispatcher.Command
import com.zhongfei.scheduler.transport.Peer
import com.zhongfei.scheduler.transport.codec.RequestProtocolDecoder
import com.zhongfei.scheduler.transport.protocol.SchedulerProtocol

class HeartBeatDecoder extends RequestProtocolDecoder[HeartBeat,Command]{

  /**
   * 执行节码操作
   *
   * @param msg 需要节码的消息
   * @return 返回节码后的对象
   */
  override def decode(msg: SchedulerProtocol.Request,peer: Peer): HeartBeat = {
    HeartBeat(msg.actionId,new String(msg.context),peer)
  }
}
