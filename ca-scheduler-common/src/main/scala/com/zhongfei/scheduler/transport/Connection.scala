package com.zhongfei.scheduler.transport

import io.netty.channel.Channel

/**
 * 网络链接抽象
 */
case class Connection(channel:Channel,node: Node)
