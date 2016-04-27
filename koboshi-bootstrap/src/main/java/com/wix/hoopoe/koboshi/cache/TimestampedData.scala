package com.wix.hoopoe.koboshi.cache

import org.joda.time.Instant

case class TimestampedData[T](data: T, lastUpdate: Instant)

