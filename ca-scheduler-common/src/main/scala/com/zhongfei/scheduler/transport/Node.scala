package com.zhongfei.scheduler.transport

object Node {
  val prefix = "scheduler://"

  def apply(host: String, port: Int): Node = new Node(host, port)

  def createByUri(uri: String): Node = {
    if (uri.startsWith(prefix)) {
      val i1 = uri.indexOf("@")
      val host = uri.substring(prefix.length, i1)
      val port = uri.substring(i1 + 1, uri.length).toInt
      new Node(host, port)
    }else{
      throw new RuntimeException("uri解析错误")
    }
  }
}

/**
 * 节点抽象，每个可部署应用都是一个节点
 */
class Node(val host: String, val port: Int) {
  def uri(): String = {
    Node.prefix + host + "@" + port
  }
}
