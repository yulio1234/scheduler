package com.zhongfei.scheduler.storage

/**
  * @Auther: yuli
  * @Date: 2019/10/11 11:34
  * @Description: k/v存储特质
  */
trait RawKVStore {
  def get(key:Array[Byte]):Array[Byte]
  def put(key:Array[Byte],value:Array[Byte]):Unit
  def delete(key:Array[Byte]):Unit
}
