package com.wix.hoopoe.koboshi.scheduler

import org.joda.time.Instant

class SystemClock extends Clock {
  def instant: Instant = new Instant
}