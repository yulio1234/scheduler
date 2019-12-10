package com.zhongfei.scheduler.transport.codec

import com.zhongfei.scheduler.transport.protocol.SchedulerProtocol.Request

/**
 * 处理请求协议的解码器
 * @tparam U 节码后的类型
 */
abstract class RequestProtocolDecoder[U,C] extends AbstractProtocolDecoder[Request,U,C]
