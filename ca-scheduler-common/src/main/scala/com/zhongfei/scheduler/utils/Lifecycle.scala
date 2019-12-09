package com.zhongfei.scheduler.utils

trait Lifecycle[I,S] {
  def init():I
  def shutdown():S
}
