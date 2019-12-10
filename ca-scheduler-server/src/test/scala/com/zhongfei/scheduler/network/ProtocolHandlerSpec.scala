package com.zhongfei.scheduler.network

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import com.zhongfei.scheduler.command.SchedulerCommand.HeartBeat
import com.zhongfei.scheduler.network.codec.{HeartBeatDecoder, RequestProtocolHandler, RequestProtocolHandlerFactory}
import com.zhongfei.scheduler.transport.Node
import com.zhongfei.scheduler.transport.protocol.SchedulerProtocol.{ActionTypeEnum, Request}
import com.zhongfei.scheduler.utils.Logging
import io.netty.channel.ChannelFuture
import org.scalatest.WordSpecLike

class ProtocolHandlerSpec extends ScalaTestWithActorTestKit with WordSpecLike with Logging {
  "网络通讯协议处理器" when{
    "请求处理器" should{
      "处理心跳请求"in{
        val actor = createTestProbe[CoreDispatcher.Command]()
        val requestProtocolHandler = RequestProtocolHandlerFactory.create(actor.ref)
        val request = Request(actionId = 1, actionType = ActionTypeEnum.HeartBeat.id.toByte)
        requestProtocolHandler.handle(request,null)
//        val server = new NettyServer(Node("127.0.0.1", 2222), requestProtocolHandler)
//        val future = server.init()
//        future.addListener((future:ChannelFuture)=>{
//          println(future.isSuccess)
//        })

      }
    }
  }

}
