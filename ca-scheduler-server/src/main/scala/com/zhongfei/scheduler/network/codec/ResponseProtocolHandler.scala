package com.zhongfei.scheduler.network.codec

import akka.actor.typed.ActorRef
import com.zhongfei.scheduler.network.ServerDispatcher.Command
import com.zhongfei.scheduler.transport.Peer
import com.zhongfei.scheduler.transport.codec.ProtocolHandler
import com.zhongfei.scheduler.transport.protocol.SchedulerProtocol.{Request, Response}
import com.zhongfei.scheduler.utils.RemotingUtil
import io.netty.channel.Channel

/**
 * 响应协议处理器
 */
class ResponseProtocolHandler(actor:ActorRef[Command])  extends ProtocolHandler[Response,Command]{
  /**
   *
   * @param message
   * @param channel
   */
  override def handle(message: Response, channel: Channel): Unit = {
  }

}