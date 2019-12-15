package com.zhongfei.scheduler.transport

trait PeerFactory {
  def create(node:Node):Peer
}
