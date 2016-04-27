package com.wix.hoopoe.koboshi.scheduler

import java.util.concurrent.ScheduledExecutorService

trait Schedulers {
  def aScheduler(namespace: String): ScheduledExecutorService
}
