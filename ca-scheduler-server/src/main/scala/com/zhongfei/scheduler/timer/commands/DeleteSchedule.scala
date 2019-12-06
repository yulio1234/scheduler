package com.zhongfei.scheduler.timer.commands

/**
 * 删除调度
 * @param id
 * @param eventName
 * @param domain
 */
case class DeleteSchedule(id:String,eventName:String,domain:String) extends Command
