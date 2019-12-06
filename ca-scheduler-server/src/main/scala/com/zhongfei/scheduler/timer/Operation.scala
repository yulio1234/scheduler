package com.zhongfei.scheduler.timer

/**
 * 调度存储
 */
trait Operation[T]{
  def save(option: T):Unit
  def delete(id:String):Unit
  def find(id:String):T
}
