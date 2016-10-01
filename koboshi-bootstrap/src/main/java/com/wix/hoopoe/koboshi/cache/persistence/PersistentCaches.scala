package com.wix.hoopoe.koboshi.cache.persistence

import com.wix.hoopoe.koboshi.marshaller.Marshaller
import com.wix.hoopoe.koboshi.report.RemoteDataFetchingReporter

trait PersistentCaches {
  def aPersistentCache[T](namespace: String, marshaller: Marshaller[T], reporter: RemoteDataFetchingReporter): PersistentCache[T]
}