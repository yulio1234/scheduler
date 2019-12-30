package com.zhongfei.scheduler.transport.protocol
import com.zhongfei.scheduler.common.Option

/**
 * 应用配置
 * @param processWaitTime
 */
case class ApplicationOption( applicationName:String,
                              processWaitTime:Long) extends Option
