package com.zhongfei.scheduler.network

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import com.zhongfei.scheduler.network.codec.{HeartBeatDecoder, RequestProtocolHandler}
import com.zhongfei.scheduler.transport.Peer
import com.zhongfei.scheduler.transport.codec.AbstractProtocolDecoder
import com.zhongfei.scheduler.utils.Logging
import org.scalatest.WordSpecLike

class ProtocolHandlerSpec extends ScalaTestWithActorTestKit with WordSpecLike with Logging {
  "协议处理器" when{
    "请求处理器" should{
//      val actorProbe = createTestProbe[CoreDispatcher.Command]()
//      val requestProtocolHandler = new RequestProtocolHandler(actorProbe.ref)
//      val decoder = new HeartBeatDecoder()
//      requestProtocolHandler.registerCommand(decoder);
      new AbstractProtocolDecoder[Int,AnyVal,Int]{
        /**
         * 执行节码操作
         *
         * @param msg 需要节码的消息
         * @return 返回节码后的对象
         */
        /**
         * 执行节码操作
         *
         * @param msg 需要节码的消息
         * @return 返回节码后的对象
         */
        override def decode(msg: Int, peer: Peer): Int = {
          1
        }
      }
      "创建心跳请求"in{

      }
    }
  }

}
