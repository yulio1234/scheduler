package com.zhongfei.scheduler.transport.protocol

import spray.json.DefaultJsonProtocol

object JsonProtocol  extends DefaultJsonProtocol {
  implicit val applicationOptionFormat = jsonFormat2(ApplicationOption)
}
