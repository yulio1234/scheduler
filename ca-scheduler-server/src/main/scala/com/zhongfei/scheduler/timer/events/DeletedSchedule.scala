package com.zhongfei.scheduler.timer.events

case class DeletedSchedule(id:String,eventName:String,domain:String) extends Event
