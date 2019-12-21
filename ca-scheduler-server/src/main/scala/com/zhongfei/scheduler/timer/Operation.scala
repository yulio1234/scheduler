package com.zhongfei.scheduler.timer

/**
 * 调度存储
 */
trait Operation[T]{
  def save(option: T):Unit
  def delete(id:Long):Unit
  def find(id:Long):T
}
