package com.wix.hoopoe.koboshi.cache

import com.wix.hoopoe.koboshi.cache.transience.TransientCache

class InitializationAwareCache[T](underlyingCache:TransientCache[T]) extends TransientCache[T]{
  @volatile private[this] var readPolicy: ()=>TimestampedData[T]  = errorOnReads

  override def read(): T = readPolicy.apply().data

  override def write(data: TimestampedData[T]): Unit = {
    underlyingCache.write(data)
    readPolicy = allowReads
  }

  private def allowReads: () => TimestampedData[T] = {
    () => underlyingCache.readTimestamped
  }
  private def errorOnReads: () => Nothing = {
    () => throw new IllegalStateException("Cannot read before a single write has happened")
  }

  override def readTimestamped(): TimestampedData[T] = readPolicy.apply()
}
