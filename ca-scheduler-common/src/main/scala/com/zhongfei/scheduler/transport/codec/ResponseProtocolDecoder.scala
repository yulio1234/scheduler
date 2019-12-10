package com.zhongfei.scheduler.transport.codec

import com.zhongfei.scheduler.transport.protocol.SchedulerProtocol.{Request, Response}

/**
 * 处理响应协议的解码器
 * @tparam U
 */
abstract class ResponseProtocolDecoder[U,C] extends AbstractProtocolDecoder[Response,U,C]
