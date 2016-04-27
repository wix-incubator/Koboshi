package com.wix.hoopoe.koboshi.cache.persistence

import com.wix.hoopoe.koboshi.marshaller.Marshaller

trait PersistentCaches {
  def aPersistentCache[T](namespace: String, marshaller: Marshaller[T]): PersistentCache[T]
}