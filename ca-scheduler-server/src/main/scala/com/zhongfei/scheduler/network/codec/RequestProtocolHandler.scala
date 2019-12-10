package com.zhongfei.scheduler.network.codec

import akka.actor.typed.ActorRef
import com.zhongfei.scheduler.network.CoreDispatcher.Command
import com.zhongfei.scheduler.transport.Peer
import com.zhongfei.scheduler.transport.codec.ProtocolHandler
import com.zhongfei.scheduler.transport.protocol.SchedulerProtocol.Request
import com.zhongfei.scheduler.utils.RemotingUtil
import io.netty.channel.Channel

/**
 * 请求消息处理器
 * @param actor
 */
class RequestProtocolHandler(actor:ActorRef[Command]) extends ProtocolHandler[Request,Command]{

  override def handle(message: Request, channel: Channel): Unit = {
    val peer = createPeer(channel)
    this.map.get(message.actionType) match {
      case Some(command: Command) => command.execute(message,actor,peer)
      case None => throw new RuntimeException("没有找到相应的消息处理器")
    }
  }
  def createPeer(channel:Channel): Peer ={
    val ip = RemotingUtil.parseRemoteIP(channel)
    val port = RemotingUtil.parseRemotePort(channel)
    Peer(ip,port,channel)
  }

}
