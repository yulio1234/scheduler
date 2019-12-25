package com.zhongfei.scheduler.executive

import akka.actor.typed.ActorRef
import com.zhongfei.scheduler.network.Application

trait LoadBalance {
  def select(list:List[ActorRef[Application.Command]]):ActorRef[Application.Command]
}
