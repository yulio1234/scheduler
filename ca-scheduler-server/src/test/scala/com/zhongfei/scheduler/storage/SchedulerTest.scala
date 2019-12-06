package com.zhongfei.scheduler.storage

import com.zhongfei.scheduler.options.RocksDbOptions

/**
  * @Auther: yuli
  * @Date: 2019/10/11 14:54
  * @Description:
  */
object SchedulerTest {
  def main(args: Array[String]): Unit = {
    val store = new RocksRawKVStore()
    store.init(RocksDbOptions("D:\\rocksDb"))

    store.put("1".getBytes,"hello".getBytes())
    val bytes = store.get("1".getBytes())


     val str = new String(bytes)
    println(str)
  }
}
