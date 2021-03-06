package com.zhongfei.scheduler.network.codec

import com.zhongfei.scheduler.Exception.SchedulerExceptionFactory
import com.zhongfei.scheduler.network.Dispatcher.{HeartBeaten, Message}
import com.zhongfei.scheduler.transport.Peer
import com.zhongfei.scheduler.transport.codec.ResponseProtocolDecoder
import com.zhongfei.scheduler.transport.protocol.SchedulerProtocol
import com.zhongfei.scheduler.utils.Logging

/**
 * 心跳解码器，处理心跳节码请求，并执行
 */
class HeartBeatenDecoder extends ResponseProtocolDecoder[HeartBeaten,Message] with Logging{

  /**
   * 执行节码操作
   *
   * @param msg 需要节码的消息
   * @return 返回节码后的对象
   */
  override def decode(msg: SchedulerProtocol.Response,peer: Peer): Option[HeartBeaten] = {
      debug(s"处理心跳响应：$msg,对等端：$peer")
      Some(HeartBeaten(msg.actionId,msg.success,SchedulerExceptionFactory.get(msg.errorCode),peer))
  }
}
