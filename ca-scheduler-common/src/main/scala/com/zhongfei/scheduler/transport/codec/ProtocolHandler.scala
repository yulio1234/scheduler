package com.zhongfei.scheduler.transport.codec

import io.netty.channel.Channel

/**
 *
 * @tparam P 可执行的消息对象
 */
abstract class ProtocolHandler[P,C] {
  var map:Map[Byte,Command[P,C]] = Map.empty

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
  def registerCommand(id:Byte,command: Command[P,C])={
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
