package com.zhongfei.scheduler.transport.codec

import com.zhongfei.scheduler.transport.Peer

/**
 * 解码处理器，处理
 *
 * @tparam P 输入的消息类型
 * @tparam U 返回的消息类型
 */
trait Decoder[P,U] {
  /**
   * 执行节码操作
   * @param msg 需要节码的消息
   * @return 返回节码后的对象
   */
  def decode(msg:P,peer: Peer):U
}
