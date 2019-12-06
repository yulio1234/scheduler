package com.zhongfei.scheduler.storage

import akka.actor.{Actor, ActorLogging}
import com.zhongfei.scheduler.timer.{Evt, ExampleState}

/**
  * @Auther: yuli
  * @Date: 2019/10/11 16:01
  * @Description: 数据库actor
  */
class RawKVStoreActor(store:RawKVStore) extends Actor with ActorLogging{
  override def receive: Receive = {
    case PutRequest(key,value) => {
      store.put(key,value)
      sender() ! PutResponse
    }
    case GetRequest(key) => {
      val value = store.get(key)
      sender() ! GetResponse(value)
    }
    case DeleteRequest(key) =>{
      store.delete(key)
      sender() ! DeleteResponse
    }
  }

}
