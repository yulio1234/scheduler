package com.zhongfei.scheduler.network.codec

import java.util

import com.alibaba.fastjson.JSON
import com.zhongfei.scheduler.network.Dispatcher.{HeartBeat, ScheduleAdd}
import com.zhongfei.scheduler.transport.protocol.SchedulerProtocol.{ActionTypeEnum, Request}
import com.zhongfei.scheduler.utils.Logging
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToMessageEncoder

class ScheduleAddToRequestEncoder extends MessageToMessageEncoder[ScheduleAdd] with Logging{
  override def encode(ctx: ChannelHandlerContext, msg: ScheduleAdd, out: util.List[AnyRef]): Unit = {
    val body:String = JSON.toJSONString(msg.body)
    val bytes = body.getBytes
    val request = Request(actionId = msg.actionId, actionType = ActionTypeEnum.ScheduleAdd.id.toByte, length = bytes.length.toShort, content = bytes)
    out.add(request)
  }
}
