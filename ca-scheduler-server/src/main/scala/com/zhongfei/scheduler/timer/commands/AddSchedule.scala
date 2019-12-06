package com.zhongfei.scheduler.timer.commands

/**
 * 添加调度命令
 * @param id
 * @param eventName
 * @param expire
 * @param domain
 */
case class AddSchedule(id:String,eventName:String,expire:Long,domain:String) extends Command
