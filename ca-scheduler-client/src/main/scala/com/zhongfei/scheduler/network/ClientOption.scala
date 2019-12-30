package com.zhongfei.scheduler.network

import com.zhongfei.scheduler.common.Option

import scala.concurrent.duration._

/**
 *
 * @param appName                       应用名称
 * @param fetchActiveServerListInterval 拉取活动服务间隔
 * @param sendHeartBeatInterval         发送心跳间隔的时间
 * @param checkHeartBeatOnCloseInterval 检查心跳间隔，并且关闭系统的时间
 * @param transferRetryCount            传输数据重试次数
 * @param transferRetryInterval         传输数据重试间隔
 * @param iniTimout                     启动超时时间
 * @param reconnectInterval             重连间隔时间
 * @param processWaitTime               执行等待时间
 */
case class ClientOption(
                         appName: String,
                         fetchActiveServerListInterval:FiniteDuration = 10.seconds,
                         sendHeartBeatInterval: FiniteDuration = 5.seconds,
                         checkHeartBeatOnCloseInterval: FiniteDuration = 8.seconds,
                         transferRetryCount: Int = 0,
                         transferRetryInterval: FiniteDuration = 500.millis,
                         transferTimeoutInterval:FiniteDuration = 3.seconds,
                         iniTimout: FiniteDuration = 2000.millis,
                         reconnectInterval: FiniteDuration = 3.seconds,
                         processWaitTime:Long = 2000
                       ) extends Option
