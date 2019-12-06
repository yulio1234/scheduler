package com.zhongfei.scheduler

/**
 * 数据转换器
 * @tparam I
 * @tparam O
 * @tparam U
 */
abstract class Converter[I,O,U] {
  def convert(message:I,attachment:U):O
}
