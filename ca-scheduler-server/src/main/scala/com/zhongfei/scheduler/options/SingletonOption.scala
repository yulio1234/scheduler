package com.zhongfei.scheduler.options

import com.zhongfei.scheduler.Option
import com.zhongfei.scheduler.transport.Node

import scala.concurrent.duration._
import scala.concurrent.duration.FiniteDuration

/**
 * 配置文件
 *
 */
case class SingletonOption(node:Node,
                           checkHeartbeatInterval:FiniteDuration = 6.seconds,
                           checkHeartBeatOnCloseInterval:FiniteDuration = 10.seconds,
                           transferRetryCount:Int = 3,
                           transferRetryInterval:FiniteDuration = 500.millis,
                           processWaitTime:FiniteDuration = 2.seconds
                          ) extends Option
