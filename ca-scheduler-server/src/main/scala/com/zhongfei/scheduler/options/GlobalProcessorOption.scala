package com.zhongfei.scheduler.options

import akka.actor.typed.ActorRef
import com.zhongfei.scheduler.registry.ApplicationManager.Command

/**
 * 全局处理器配置文件
 *
 * @param startModel
 */
case class GlobalProcessorOption(startModel:Byte,appManagerActor: ActorRef[Command]) extends Option
