package com.zhongfei.scheduler.network

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import com.zhongfei.scheduler.network.SchedulerConnection.Initialize
import com.zhongfei.scheduler.network.SchedulerConnectionManager._
import com.zhongfei.scheduler.transport.Node

object SchedulerConnectionManager {

  trait Message

  trait Event extends Message

  trait Command extends Message

  //连接服务
  case class Connects(nodes: Array[Node]) extends Command with SchedulerConnection.Command
  case class Unreachable(serverKey:String) extends Command with SchedulerConnection.Command
  //连接服务完成
  case class Connected(serverKey: String, serverRef: ActorRef[SchedulerConnection.Command]) extends Command

  //不可达命令
  case class ServerUnreachable(serverKey: String) extends Command with SchedulerConnection.Command
  case object ServerTerminate extends Command with SchedulerConnection.Command
  case class ServerTerminated(serverKey: String) extends Event

  case class ActiveServerList(hosts: Option[Set[Node]]) extends Event
  case class ServerActive(serverKey:String) extends Event
  def apply(option: ClientOption): Behavior[Message] = Behaviors.setup { context =>
    new SchedulerConnectionManager(option, context).down()
  }
}

/**
 * 只负责管理连接，不负责处理各种事件
 *
 * @param option
 * @param context
 */
class SchedulerConnectionManager(option: ClientOption, context: ActorContext[Message]) {
  /**
   * 下线状态
   *
   * @return
   */
  def down(): Behavior[Message] = Behaviors.receiveMessage {
        //连接服务端
    case Connects(nodes) =>
      nodes.foreach(node => tryConnect(node))
      Behaviors.same
      //如果有接收到服务端应答，就转换状态到上线
    case Connected(serverKey, serverRef) =>
      up(Map.empty, Map.empty + (serverKey -> serverRef))
  }

  /**
   * 上线状态
   *
   * @param waitOnlineServer 等待上下的应用
   * @param onlineServer
   * @return
   */
  def up(waitOnlineServer: Map[String, ActorRef[SchedulerConnection.Command]], onlineServer: Map[String, ActorRef[SchedulerConnection.Command]]): Behavior[Message] = Behaviors.receiveMessage{
        //注册完成
    case Connected(serverKey,serverRef)=>
      up(waitOnlineServer,onlineServer + (serverKey->serverRef))
    //活跃的服务
    case ActiveServerList(hosts) =>
      hosts match {
        case Some(nodes) =>
          //当前活跃的服务
          val activeServerKeys = nodes.collect(node => node.uri())
          //活跃服务减去在线服务等于需要上线的服务
          val waitOnline = activeServerKeys -- onlineServer.keySet
          //在线服务减去活跃服务（不再活跃服务里的服务）等于需要下线的服务
          val doOffline = onlineServer.keySet -- activeServerKeys
          //发送下线通知
          doOffline.foreach(key => {
            val server = onlineServer(key)
            server ! ServerTerminate
          })
          //如果需要上线的服务，不在等待上线的服务里，就上线
          val doOnline = waitOnline -- waitOnlineServer.keySet
          doOnline.foreach(uri => tryConnect(Node.createByUri(uri)))
          //
          val allDoOffline = waitOnlineServer.keySet -- (doOnline ++ waitOnline)
          allDoOffline.foreach(key =>{
            waitOnlineServer(key) ! ServerTerminate
          })
          Behaviors.same
        case None =>Behaviors.same
      }
    //接收服务发来的不可达消息
    case ServerUnreachable(serverKey) =>
      //将不可达的服务转移到等待上线中
      up(waitOnlineServer + (serverKey -> onlineServer(serverKey)),onlineServer - serverKey)
      //服务重新连上线了
    case ServerActive(serverKey) =>
      up(waitOnlineServer - serverKey,onlineServer + (serverKey -> waitOnlineServer(serverKey)))
    case ServerTerminated(serverKey) =>
      //注销已经关闭的服务
      up(waitOnlineServer -serverKey, onlineServer - serverKey)

  }

  /**
   * 每次心跳服务器返回可以用服务列表，比较可以服务列表和在线服务列表，如果有不在线的，就更新为最新的，并尝试链接。
   * 至于服务器返回的在线列表是否一致，服务器采用共识算法（quorum）保证每次数据返回的正确性
   *
   * @param waitOnlineServer
   */
  def tryConnect(node: Node): Unit = {
    val connection = context.spawn(SchedulerConnection(option, node, context.self), "server-" + node.uri())
    context.watchWith(connection, ServerTerminated(node.uri()))
    connection ! Initialize
  }
}
