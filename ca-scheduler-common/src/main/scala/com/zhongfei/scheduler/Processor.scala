package com.zhongfei.scheduler

/**
 * 全局执行器抽象，负责各种事务的执行，譬如消息接收后的事务处理，调度任务到期后的事件分发。全局唯一一个，保证事务处理
 */
abstract class Processor[T] {
 def process:T
}
