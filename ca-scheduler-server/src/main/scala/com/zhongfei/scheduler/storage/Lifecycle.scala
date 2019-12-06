package com.zhongfei.scheduler.storage

/**
  * @Auther: yuli
  * @Date: 2019/10/11 14:14
  * @Description: 生命周期
  */
trait Lifecycle[T] {
  def init(opts:T):Boolean
  def shutdown:Unit
}
