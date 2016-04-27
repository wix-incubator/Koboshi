package com.wix.hoopoe.koboshi.scheduler

import java.util.concurrent.{Executors, ScheduledExecutorService}

class JavaUtilExecutorsSchedulers extends Schedulers {
  override def aScheduler(namespace: String): ScheduledExecutorService = Executors.newScheduledThreadPool(1)
}
