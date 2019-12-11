package com.zhongfei.scheduler.network

import com.zhongfei.scheduler.Option
import scala.concurrent.duration._

/**
 *
 * @param sendHeartBeatInterval 发送心跳间隔的时间
 * @param checkHeartBeatOnCloseInterval 检查心跳间隔，并且关闭系统的时间
 * @param transferRetryCount 传输数据重试次数
 * @param transferRetryInterval 传输数据重试间隔
 */
case class ClientOption(sendHeartBeatInterval: FiniteDuration = 8.seconds,
                        checkHeartBeatOnCloseInterval:FiniteDuration = 10.seconds,
                        transferRetryCount: Int = 3,
                        transferRetryInterval: FiniteDuration = 500.millis,
                       ) extends Option
