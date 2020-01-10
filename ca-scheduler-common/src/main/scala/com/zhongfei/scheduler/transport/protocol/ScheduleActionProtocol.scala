package com.zhongfei.scheduler.transport.protocol

import akka.actor.typed.ActorRef
import com.zhongfei.scheduler.transport.Peer
import com.zhongfei.scheduler.transport.protocol.SchedulerProtocol.ActionTypeEnum
import com.zhongfei.scheduler.utils.IDGenerator

object ScheduleActionProtocol {
  case class OperationResult(actionId:Long,actionType:ActionTypeEnum.Value,success:Boolean,cause:Throwable)
  case class ScheduleAdd(actionId:Long,body:ScheduleBody,expire:Long,peer: Peer,replyTo: ActorRef[OperationResult])
  case class HeartBeat(actionId: Long = IDGenerator.next(),applicationOption: ApplicationOption, peer: Peer, replyTo: ActorRef[OperationResult])
  case class Unregister(actionId: Long, appName: String,peer: Peer,replyTo:ActorRef[OperationResult])
  case class ScheduleDel(actionId:Long,replyTo:ActorRef[OperationResult])
  case class ScheduleBody(id:Long,domain:String,eventName:String,extra:String)
}
