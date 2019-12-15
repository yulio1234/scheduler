package com.zhongfei.scheduler.network.codec

import com.zhongfei.scheduler.network.Command.SchedulerCommand.Unregistered
import com.zhongfei.scheduler.network.SchedulerConnectionManager.Message
import com.zhongfei.scheduler.transport.Peer
import com.zhongfei.scheduler.transport.codec.ResponseProtocolDecoder
import com.zhongfei.scheduler.transport.protocol.SchedulerProtocol
import com.zhongfei.scheduler.utils.Logging

/**
 * 取消注册响应解码器
 */
class UnregisteredDecoder extends ResponseProtocolDecoder[Unregistered,Message] with Logging{
  /**
   * 执行节码操作
   *
   * @param msg 需要节码的消息
   * @return 返回解码后的对象
   */
  override def decode(msg: SchedulerProtocol.Response, peer: Peer): Option[Unregistered] = {
    if (msg.length > 0) {
      debug(s"处理取消注册请求：$msg,对等端：$Peer")
      Some(Unregistered(actionId = msg.actionId))
    }else{
      None
    }

  }
}
