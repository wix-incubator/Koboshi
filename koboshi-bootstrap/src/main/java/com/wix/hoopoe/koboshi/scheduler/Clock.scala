package com.wix.hoopoe.koboshi.scheduler

import org.joda.time.Instant

trait Clock {
  def instant: Instant
}