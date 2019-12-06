package com.zhongfei.scheduler.processor

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import com.zhongfei.scheduler.Processor
import com.zhongfei.scheduler.processor.AppManagerProcessor.Command
import com.zhongfei.scheduler.registry.ApplicationManager

/**
 *
 */
object AppManagerProcessor{
  trait Command
}
/**
 * 处理应用管理器相关事务
 */
private class AppManagerProcessor(replyTo:ActorRef[GlobalProcessor.Command],transferTo:ActorRef[ApplicationManager.Command]) extends Processor[Behavior[Command]]{
  override def process: Behavior[Command] = {
    Behaviors.same
  }
}
