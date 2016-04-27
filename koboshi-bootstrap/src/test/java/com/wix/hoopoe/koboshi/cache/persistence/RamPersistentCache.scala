package com.wix.hoopoe.koboshi.cache.persistence

import com.wix.hoopoe.koboshi.cache.TimestampedData
import com.wix.hoopoe.koboshi.cache.transience.{AtomicReferenceCache, TransientCache}

class RamPersistentCache[T] extends PersistentCache[T] {
  private val c: TransientCache[T] = new AtomicReferenceCache[T]

  override def readTimestamped(): TimestampedData[T] = c.readTimestamped

  override def write(timestampedData: TimestampedData[T]): Unit = c.write(timestampedData)
}

