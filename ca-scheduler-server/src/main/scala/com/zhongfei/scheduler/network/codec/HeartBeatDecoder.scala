package com.zhongfei.scheduler.network.codec

import com.zhongfei.scheduler.network.ApplicationManager.HeartBeat
import com.zhongfei.scheduler.network.ServerDispatcher.{Command, WrappedHeartBeat}
import com.zhongfei.scheduler.transport.Peer
import com.zhongfei.scheduler.transport.codec.RequestProtocolDecoder
import com.zhongfei.scheduler.transport.protocol.{ApplicationOption, SchedulerProtocol}
import com.zhongfei.scheduler.utils.Logging
import spray.json._
import com.zhongfei.scheduler.transport.protocol.JsonProtocol.applicationOptionFormat

/**
 * 心跳解码器，处理心跳节码请求，并执行
 */
class HeartBeatDecoder extends RequestProtocolDecoder[WrappedHeartBeat,Command] with Logging{

  /**
   * 执行节码操作
   *
   * @param msg 需要节码的消息
   * @return 返回节码后的对象
   */
  override def decode(msg: SchedulerProtocol.Request,peer: Peer): Option[WrappedHeartBeat] = {
    if (msg.length.toInt > 0) {
      debug(s"处理心跳请求：${new String(msg.content)},对等端：${Peer}")
      val json = new String(msg.content).parseJson
      val option = json.convertTo[ApplicationOption]
      Some(WrappedHeartBeat(HeartBeat(msg.actionId,option,peer,null)))
    }else{
      None
    }
  }
}
