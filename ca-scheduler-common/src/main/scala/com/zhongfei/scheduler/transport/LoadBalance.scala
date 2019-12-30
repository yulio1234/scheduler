package com.zhongfei.scheduler.transport

trait LoadBalance[T] {
  def select(list:List[T]):Option[T]
}
