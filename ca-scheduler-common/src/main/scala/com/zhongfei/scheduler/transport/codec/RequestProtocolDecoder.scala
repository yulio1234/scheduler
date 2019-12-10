package com.zhongfei.scheduler.transport.codec


import com.zhongfei.scheduler.transport.protocol.SchedulerProtocol.Request

/**
/ * 处理请求协议的解码器
 * @tparam U 返回的消息类型，用于给actor发送的actor内部子协议
 * @tparam C actor内部的内部协议父类，用于接收所有子协议
 */
abstract class RequestProtocolDecoder[U<:C,C] extends AbstractProtocolDecoder[Request,U,C]
