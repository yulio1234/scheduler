package com.zhongfei.scheduler.network
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, TimerScheduler}
import akka.actor.typed.{ActorRef, Behavior}
import com.zhongfei.scheduler.network.SchedulerConnection.Initialize
import com.zhongfei.scheduler.network.SchedulerConnectionManager._
import com.zhongfei.scheduler.network.codec.{RequestProtocolHandlerFactory, ResponseProtocolHandlerFactory}
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
  //抓取活跃服务列表
  case object FetchActiveServerList extends Command

  //服务端不可达
  case class ServerUnreachable(serverKey: String) extends Command with SchedulerConnection.Command
  //服务停止请求
  case object ServerTerminate extends Command with SchedulerConnection.Command
  //服务已经停止
  case class ServerTerminated(serverKey: String) extends Event
  //活动服务列表
  case class ActiveServerList(hosts: Option[Set[Node]]) extends Event
  //活动服务
  case class ServerActive(serverKey:String) extends Event
  //服务管理器停止
  case object ServerManagerTerminate extends Command
  //查询在线服务列表
  case class OnlineServerListQuery(actorRef:ActorRef[OnlineServerListQueried]) extends Command
  //返回在线服务列表
  case class OnlineServerListQueried(set:Set[ActorRef[SchedulerConnection.Command]]) extends Event

  def apply(option: ClientOption,dispatcher: ActorRef[Dispatcher.Message]): Behavior[Message] = Behaviors.setup { context => Behaviors.withTimers{
    timers =>
    val client = new SchedulerClient(RequestProtocolHandlerFactory.create(dispatcher), ResponseProtocolHandlerFactory.create(dispatcher))
      new SchedulerConnectionManager(option,timers, dispatcher,context).down(client)
  }
  }
}

/**
 * 只负责管理连接，不负责处理各种事件
 *
 * @param option
 * @param context
 */
class SchedulerConnectionManager(option: ClientOption,  timers: TimerScheduler[Message], dispatcher: ActorRef[Dispatcher.Message],context: ActorContext[Message]){
  /**
   * 下线状态
   *
   * @return
   */
  def down(schedulerClient: SchedulerClient): Behavior[Message] = Behaviors.receiveMessage {
        //连接服务端
    case Connects(nodes) =>
      nodes.foreach(node => tryConnect(node,schedulerClient))
      down(schedulerClient)
      //如果有接收到服务端应答，就转换状态到上线
    case Connected(serverKey, serverRef) =>
      //定时拉去活跃服务列表
      timers.startTimerWithFixedDelay(FetchActiveServerList,FetchActiveServerList,option.fetchActiveServerListInterval)
      //监听服务下线
      context.watchWith(serverRef,ServerTerminated(serverKey))
      up(Map.empty, Map.empty + (serverKey -> serverRef),schedulerClient)
  }

  /**
   * 上线状态
   *
   * @param waitOnlineServer 等待上下的应用
   * @param onlineServer
   * @return
   */
  def up(waitOnlineServer: Map[String, ActorRef[SchedulerConnection.Command]],
         onlineServer: Map[String, ActorRef[SchedulerConnection.Command]],
           schedulerClient: SchedulerClient
        ): Behavior[Message] = Behaviors.receiveMessage{
        //注册完成
    case Connected(serverKey,serverRef)=>
      //监听服务下线
      context.watchWith(serverRef,ServerTerminated(serverKey))
      up(waitOnlineServer-serverKey,onlineServer + (serverKey->serverRef),schedulerClient)
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
          doOnline.foreach(uri => tryConnect(Node.createByUri(uri),schedulerClient))
          //
          val allDoOffline = waitOnlineServer.keySet -- (doOnline ++ waitOnline)
          allDoOffline.foreach(key =>{
            waitOnlineServer(key) ! ServerTerminate
          })
          Behaviors.same
        case None =>Behaviors.same
      }
      //查询在线服务列表
    case OnlineServerListQuery(actorRef) =>
      actorRef ! OnlineServerListQueried(onlineServer.values.toSet)
      Behaviors.same
    //接收服务发来的不可达消息
    case ServerUnreachable(serverKey) =>
      context.log.debug(s"收到服务不可达消息，serverKey = $serverKey,当前不可达服务容器 = ${waitOnlineServer.keys}，当前在线服务容器 = ${onlineServer.keys}")
      //将不可达的服务转移到等待上线中
      up(waitOnlineServer + (serverKey -> onlineServer(serverKey)),onlineServer - serverKey,schedulerClient)
      //服务重新连上线了
    case ServerActive(serverKey) =>
      up(waitOnlineServer - serverKey,onlineServer + (serverKey -> waitOnlineServer(serverKey)),schedulerClient)
    case ServerTerminated(serverKey) =>
      context.log.debug(s"收到服务关闭消息，serverKey = $serverKey,当前不可达容器 = ${waitOnlineServer.keys} ，当前在线服务容器 = ${onlineServer.keys}")
      //注销已经关闭的服务
      up(waitOnlineServer -serverKey, onlineServer - serverKey,schedulerClient)
    case ServerManagerTerminate =>
      //通知所有服务下线
      context.log.debug("接收到服务管理器下线请求")
      onlineServer.values.foreach{server =>
        server ! ServerTerminate
      }
      down(null)

  }



  /**
   * 尝试连接服务器
   * @param node
   */
  def tryConnect(node: Node,schedulerClient: SchedulerClient): Unit = {
    val connection = context.spawn(SchedulerConnection(option, node, context.self,dispatcher,schedulerClient), "server-" + node.uri())
    context.watchWith(connection, ServerTerminated(node.uri()))
    connection ! Initialize
  }
}
