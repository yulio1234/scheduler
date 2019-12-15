package com.zhongfei.scheduler.transport.codec

import akka.actor.typed.ActorRef
import com.zhongfei.scheduler.transport.Peer
import com.zhongfei.scheduler.utils.RemotingUtil
import io.netty.channel.Channel

/**
 * 协议处理器抽象类
 * @tparam P
 * @tparam C
 */
abstract class ProtocolHandler[P,C] {
  var map:Map[Int,Command[P,C]] = Map.empty
  def doHandler(message:P,channel: Channel):Unit
  /**
   * 处理消息
   * @param message
   * @param channel
   */
  def handle(id:Int,message: P,actorRef: ActorRef[C] ,channel: Channel): Unit = {
    this.map.get(id) match {
      case Some(command) =>
        command.execute(message,actorRef,createPeer(channel))
      case None => throw new RuntimeException("没有找到相应的消息处理器")
    }
  }
  def createPeer(channel:Channel): Peer ={
    val ip = RemotingUtil.parseRemoteIP(channel)
    val port = RemotingUtil.parseRemotePort(channel)
    Peer(ip,port,channel)
  }
  /**
   * 注册命令处理器
   * @param id
   * @param command
   */
  def registerCommand(id:Int,command: Command[P,C])={
    map += (id->command)
  }

  /**
   * 取消注册命令处理器
   * @param id
   */
  def unregisterCommand(id:Byte): Unit ={
    map -= id
  }
}
