package com.zhongfei.scheduler.transport.protocol
import spray.json._
object Test extends App {

  import com.zhongfei.scheduler.transport.protocol.JsonProtocol.applicationOptionFormat
  val json = ApplicationOption("a",1).toJson
  println(json)
  private val str: String = json.toString()
  private val json1: JsValue = str.parseJson
  private val option: ApplicationOption = json1.convertTo[ApplicationOption]
  println(option.applicationName)
}
