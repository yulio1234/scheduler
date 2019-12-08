package com.zhongfei.scheduler.transport
object Node{
  def apply(host: String, port: Int): Node = new Node(host, port)
}
/**
 * 节点抽象，每个可部署应用都是一个节点
 */
class Node(val host:String,val port:Int){
  def uri(): String ={
    "scheduler://"+host+"@"+port
  }
}
