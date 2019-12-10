package com.zhongfei.scheduler.transport.codec

import io.netty.channel.Channel

/**
 * 协议处理器抽象类
 * @tparam P
 * @tparam C
 */
abstract class ProtocolHandler[P,C] {
  var map:Map[Int,Command[P,C]] = Map.empty

  /**
   * 处理消息
   * @param message
   * @param channel
   */
  def handle(message:P,channel:Channel):Unit

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
