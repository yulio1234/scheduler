package com.zhongfei.scheduler.timer

import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean

import org.junit.{After, Before, Test}

import scala.collection.mutable.ArrayBuffer

class TimerTest {
  private class TestTask(override val delayMs:Long,id:Int,latch:CountDownLatch,output:ArrayBuffer[Int]) extends TimerTask{
    private[this] val completed = new AtomicBoolean(false)
    override def run(): Unit = {
      if(completed.compareAndSet(false,true)){
        output.synchronized(output+=id)
        println("It's :" + id)
//        latch.countDown()
      }
    }
  }

  private[this] var timer:Timer = null;
  @Before
  def setup()={
    timer = new SystemTimer("test")
  }
  @After
  def teardown()={
    timer.shutdown()
  }
  @Test
  def testAlreadyExpiredTask()={
    val output = new ArrayBuffer[Int]()
    (-5 until 0).foreach(i=>{
      val latch = new CountDownLatch(1)
      timer.add(new TestTask(i,i,latch,output))
    })
    timer.advanceClock(0)
    (0 until 100).foreach(i=>{
      val latch = new CountDownLatch(1)
      timer.add(new TestTask(i,i,latch,output))
    })
    timer.advanceClock(0)
    Thread.sleep(1000)
//    latches.take(5).foreach(latch =>{
//      assertEquals("already expired takes should run immediately",true,latch.await(3,TimeUnit.SECONDS))
//    })
//    assertEquals("output of already expired takes",Set(-5,-4,-3,-2,-1),output.toSet)
  }



}
