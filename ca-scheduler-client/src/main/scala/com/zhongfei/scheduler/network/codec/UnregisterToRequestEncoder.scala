package com.zhongfei.scheduler.network.codec

import java.util

import com.zhongfei.scheduler.network.Dispatcher.Unregister
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToMessageEncoder

class UnregisterToRequestEncoder extends MessageToMessageEncoder[Unregister]{
  override def encode(ctx: ChannelHandlerContext, msg: Unregister, out: util.List[AnyRef]): Unit = {

  }
}
