package com.zhongfei.scheduler.timer.events

case class AddedSchedule(id:String,eventName:String,expire:Long,domain:String) extends Event
