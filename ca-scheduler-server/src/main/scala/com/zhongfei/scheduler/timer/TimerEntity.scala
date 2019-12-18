package com.zhongfei.scheduler.timer

import akka.actor.typed.ActorRef

/**
 * 定时器实体
 */
class TimerEntity {


  /**
   * 命令接口
   * @tparam Reply
   */
  sealed trait Command[Reply <: CommandReply] {
    def replyTo: ActorRef[Reply]
  }
  //返回的命令类型
  sealed trait CommandReply

  sealed trait Event

  //定时器值对象接口
  sealed trait Timer{
    def applyCommand()
    def applyEvent()
  }



}
