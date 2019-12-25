package com.zhongfei.scheduler.transport

import io.netty.channel.{Channel, ChannelFuture}

/**
 * 对等体抽象，网络连接的另一方
 */
case class Peer(override val host:String, override val port: Int, channel: Channel) extends Node(host,port){
  def close(): Unit ={
    if (channel != null && channel.isActive) {
      channel.close()
    }
  }
  def send(obj:Object): ChannelFuture ={
    channel.writeAndFlush(obj)
  }
}