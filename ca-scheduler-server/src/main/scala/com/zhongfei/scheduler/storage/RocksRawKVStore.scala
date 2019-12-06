package com.zhongfei.scheduler.storage

import com.zhongfei.scheduler.options.RocksDbOptions
import org.rocksdb.RocksDB

/**
  * @Auther: yuli
  * @Date: 2019/10/11 14:11
  * @Description:
  */
class RocksRawKVStore(var db:RocksDB = null) extends RawKVStore with Lifecycle[RocksDbOptions]{
  RocksDB.loadLibrary()
  override def init(opts: RocksDbOptions): Boolean = {
    if(db != null){
      return true;
    }
    this.db = RocksDB.open(opts.dbPath)
    true
  }
  override def get(key: Array[Byte]): Array[Byte] = this.db.get(key)
  override def put(key: Array[Byte], value: Array[Byte]): Unit = this.db.put(key,value)
  override def delete(key: Array[Byte]): Unit = this.db.delete(key)
  override def shutdown: Unit = {
    if(this.db != null){
      this.db.close()
      this.db = null
    }
  }
}
