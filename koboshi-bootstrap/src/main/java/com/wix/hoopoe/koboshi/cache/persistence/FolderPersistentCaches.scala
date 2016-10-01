package com.wix.hoopoe.koboshi.cache.persistence

import java.io.File

import com.wix.hoopoe.koboshi.marshaller.Marshaller
import com.wix.hoopoe.koboshi.report.RemoteDataFetchingReporter

class FolderPersistentCaches(cacheFolder: File) extends PersistentCaches {

  private def aFileFor(cacheFileName: String) = new File(cacheFolder, cacheFileName).toPath

  private def fileName(namespace: String) = s"koboshi.$namespace.cache"

  override def aPersistentCache[T](namespace: String, marshaller: Marshaller[T], reporter: RemoteDataFetchingReporter): PersistentCache[T] =
    new DiskCache[T](aFileFor(fileName(namespace)), marshaller, reporter)
}