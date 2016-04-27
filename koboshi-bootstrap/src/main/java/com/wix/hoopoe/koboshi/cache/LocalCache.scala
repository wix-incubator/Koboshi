package com.wix.hoopoe.koboshi.cache

trait TimestampedLocalCache[T] extends ReadOnlyTimestampedLocalCache[T] {
  def write(data: TimestampedData[T]): Unit
}