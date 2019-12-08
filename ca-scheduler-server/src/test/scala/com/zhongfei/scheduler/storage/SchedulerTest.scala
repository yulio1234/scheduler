package com.zhongfei.scheduler.storage

import java.time.LocalTime

import akka.pattern.CircuitBreaker

import scala.concurrent.{ExecutionContext, Future}
import ExecutionContext.Implicits.global
/**
  * @Auther: yuli
  * @Date: 2019/10/11 14:54
  * @Description:
  */
object SchedulerTest {
  def main(args: Array[String]): Unit = {
    Future{
      Thread.sleep(1000)
      println(s"This is thepresentat ${LocalTime.now()}")
    }
  }
}
