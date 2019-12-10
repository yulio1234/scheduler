package com.zhongfei.scheduler.network.codec

import com.zhongfei.scheduler.command.SchedulerCommand.HeartBeat
import com.zhongfei.scheduler.network.CoreDispatcher.Command
import com.zhongfei.scheduler.transport.Peer
import com.zhongfei.scheduler.transport.codec.RequestProtocolDecoder
import com.zhongfei.scheduler.transport.protocol.SchedulerProtocol
import com.zhongfei.scheduler.utils.Logging

class HeartBeatDecoder extends RequestProtocolDecoder[HeartBeat,Command] with Logging{

  /**
   * 执行节码操作
   *
   * @param msg 需要节码的消息
   * @return 返回节码后的对象
   */
  override def decode(msg: SchedulerProtocol.Request,peer: Peer): HeartBeat = {
    if (msg.length != null && msg.length > 0) {
      HeartBeat(msg.actionId,new String(msg.context),peer)
    }else{
      warn(s"应用心跳包缺少应用名称:$peer")
      null
    }

  }
}
