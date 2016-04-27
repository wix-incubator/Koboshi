package com.wix.hoopoe.koboshi.cache

trait ReadOnlyLocalCache[T] extends Cache[T] {
   def read(): T
}
trait ReadOnlyTimestampedLocalCache[T] extends Cache[T] {
  def readTimestamped(): TimestampedData[T]
}
private[koboshi] trait Cache[T]