package com.zhongfei.scheduler.network.codec

import com.zhongfei.scheduler.command.SchedulerCommand.{HeartBeat, Unregister}
import com.zhongfei.scheduler.network.ServerDispatcher.Command
import com.zhongfei.scheduler.transport.Peer
import com.zhongfei.scheduler.transport.codec.RequestProtocolDecoder
import com.zhongfei.scheduler.transport.protocol.SchedulerProtocol
import com.zhongfei.scheduler.utils.Logging

class UnregisterDecoder extends RequestProtocolDecoder[Unregister,Command] with Logging{
  /**
   * 执行节码操作
   *
   * @param msg 需要节码的消息
   * @return 返回解码后的对象
   */
  override def decode(msg: SchedulerProtocol.Request, peer: Peer): Option[Unregister] = {
    if (msg.length > 0) {
      Some(Unregister(actionId = msg.actionId,appName = new String(msg.context),peer,null))
    }else{
      None
    }

  }
}
