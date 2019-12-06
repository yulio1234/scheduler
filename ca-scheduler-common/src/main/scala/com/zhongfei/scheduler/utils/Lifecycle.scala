package com.zhongfei.scheduler.utils

trait Lifecycle {
  def init():Unit
  def shutdown():Unit
}
