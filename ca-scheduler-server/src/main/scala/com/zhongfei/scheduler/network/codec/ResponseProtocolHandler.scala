package com.zhongfei.scheduler.network.codec

import com.zhongfei.scheduler.network.ServerDispatcher.Command
import com.zhongfei.scheduler.transport.codec.ProtocolHandler
import com.zhongfei.scheduler.transport.protocol.SchedulerProtocol.{Request, Response}
import io.netty.channel.Channel

protected class ResponseProtocolHandler  extends ProtocolHandler[Response,Command]{
  /**
   * 处理消息
   *
   * @param message
   * @param channel
   */
  override def handle(message: Response, channel: Channel): Unit = {

  }
}
