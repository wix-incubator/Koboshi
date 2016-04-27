package com.wix.hoopoe.koboshi.scheduler

import org.joda.time.Instant

class FakeClock(private var now: Instant = new Instant) extends Clock {

 def instant(): Instant = now

 def setCurrent(instant: Instant) = {
   now = instant
 }
}
