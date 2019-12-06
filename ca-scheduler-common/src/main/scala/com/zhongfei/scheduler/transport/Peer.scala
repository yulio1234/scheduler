package com.zhongfei.scheduler.transport

import io.netty.channel.Channel

/**
 * 对等体抽象，网络连接的另一方
 */
case class Peer(override val host:String, override val port: Int, channel: Channel) extends Node(host,port)