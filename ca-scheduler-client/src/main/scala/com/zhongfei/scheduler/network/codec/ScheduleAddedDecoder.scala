package com.zhongfei.scheduler.network.codec

import com.zhongfei.scheduler.Exception.SchedulerExceptionFactory
import com.zhongfei.scheduler.network.Dispatcher.{Message, ScheduleAdded}
import com.zhongfei.scheduler.transport.Peer
import com.zhongfei.scheduler.transport.codec.ResponseProtocolDecoder
import com.zhongfei.scheduler.transport.protocol.SchedulerProtocol
import com.zhongfei.scheduler.utils.Logging

/**
 * 注册调度解码器
 */
class ScheduleAddedDecoder extends ResponseProtocolDecoder[ScheduleAdded,Message] with Logging{
  /**
   * 执行节码操作
   *
   * @param msg 需要节码的消息
   * @return 返回解码后的对象
   */
  override def decode(msg: SchedulerProtocol.Response, peer: Peer): Option[ScheduleAdded] = {
    Some(ScheduleAdded(msg.actionId,msg.success,SchedulerExceptionFactory.get(msg.errorCode),peer))
  }
}
