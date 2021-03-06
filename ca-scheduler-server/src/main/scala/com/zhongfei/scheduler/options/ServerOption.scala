package com.zhongfei.scheduler.options

import com.zhongfei.scheduler.common.Option
import com.zhongfei.scheduler.transport.Node

import scala.concurrent.duration._

/**
 * 配置文件
 *
 */
case class ServerOption(node: Node,
                        scheduleExpireActionInterval:FiniteDuration = 1.seconds,
                        scheduleActionInterval:FiniteDuration = 2.seconds,
                        checkHeartbeatInterval: FiniteDuration = 6.seconds,
                        checkHeartBeatOnCloseInterval: FiniteDuration = 10.seconds,
                        transferRetryCount: Int = 3,
                        transferRetryInterval: FiniteDuration = 500.millis,
                        processWaitTime: FiniteDuration = 2.seconds,
                        cluster:Boolean = true
                       ) extends Option
