package com.zhongfei.scheduler.registry
import scala.concurrent.duration._
object Application {
  def main(args: Array[String]): Unit = {
    println(10.seconds.fromNow)
  }

}
