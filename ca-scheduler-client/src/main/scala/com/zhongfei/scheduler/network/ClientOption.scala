package com.zhongfei.scheduler.network

import com.zhongfei.scheduler.Option
import scala.concurrent.duration._

case class ClientOption(heartBeatInterval: FiniteDuration,
                        transferRetryCount: Int = 3,
                        transferRetryInterval: FiniteDuration = 500.millis,
                       ) extends Option
