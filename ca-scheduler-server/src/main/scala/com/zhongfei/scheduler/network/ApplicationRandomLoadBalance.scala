package com.zhongfei.scheduler.network

import akka.actor.typed.ActorRef
import com.zhongfei.scheduler.transport.LoadBalance

import scala.util.Random

object ApplicationRandomLoadBalance extends LoadBalance[ActorRef[Application.Command]]{
  override def select(list: List[ActorRef[Application.Command]]): Option[ActorRef[Application.Command]] = {
    if(list.size < 1){
      None
    }else{
      val i = Random.nextInt(list.size)
      Some(list(i))
    }
  }
}
