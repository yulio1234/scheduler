package com.zhongfei.scheduler.timer.commands

/**
 * 调度
 */
object Schedule {
  trait Command
  final case class Add(id:String,eventName:String,expire:Long,domain:String) extends Command
  final case class Delete(id:String,eventName:String,domain:String) extends Command
  trait Event
  final case class Added(id:String,eventName:String,expire:Long,domain:String) extends Event
  final case class Deleted(id:String,eventName:String,domain:String) extends Event
}
