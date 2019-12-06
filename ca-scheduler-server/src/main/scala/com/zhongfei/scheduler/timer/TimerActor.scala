package com.zhongfei.scheduler.timer

import akka.persistence.{PersistentActor, SnapshotOffer}

/**
  * @Auther: yuli
  * @Date: 2019/10/12 16:15
  * @Description:
  */

case class Cmd(data:String)
case class Evt(data:String)

case class ExampleState(events:List[String] = Nil){
  def update(evt:Evt):ExampleState = copy(evt.data :: events)
  def size:Int = events.length
  override def toString: String = events.reverse.toString
}

class TimerActor extends PersistentActor{

  var state = ExampleState()

  def updateState(event:Evt):Unit =state = state.update(event)
  def numEvents = state.size


  override def receiveRecover: Receive = {
    case evt: Evt => updateState(evt)
    case SnapshotOffer(_,snapshot:ExampleState) => state = snapshot
  }

  val snapShotInterval = 1000
  override def receiveCommand: Receive = {
    case Cmd(data) => persist(Evt(s"${data}-${numEvents}")){event =>
      updateState(event)
      context.system.eventStream.publish(event)
      if(lastSequenceNr % snapShotInterval == 0 && lastSequenceNr != 0){
        saveSnapshot(state)
      }
    }
  }

  override def persistenceId: String = "sample-id-1"
}
