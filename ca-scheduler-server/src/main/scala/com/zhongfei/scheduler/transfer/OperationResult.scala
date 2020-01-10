package com.zhongfei.scheduler.transfer

import com.zhongfei.scheduler.timer.ScheduleBroker

trait OperationResult extends CommandReply with Transfer.Command with ScheduleBroker.Command