package com.wix.hoopoe.koboshi.report

import java.net.URI

/**
 * @author ittaiz
 * @since 12/29/13
 */
class DelegatingRemoteDataFetcherReporter(reporters: RemoteDataFetchingReporter*) extends RemoteDataFetchingReporter {
  override def cannotCompleteFetchingFromRemote(e: Exception): Unit  =
    reporters.foreach(_.cannotCompleteFetchingFromRemote(e))

  override def attemptingToFetchFromRemote(): Unit =
    reporters.foreach(_.attemptingToFetchFromRemote())

  override def cannotReadFromPersistentCache(exception: RuntimeException): Unit =
    reporters.foreach(_.cannotReadFromPersistentCache(exception))

  override def cannotWriteToPersistentCache(exception: RuntimeException): Unit =
    reporters.foreach(_.cannotWriteToPersistentCache(exception))

  override def cannotCompleteInitializingFromRemote(exception: RuntimeException): Unit =
    reporters.foreach(_.cannotCompleteInitializingFromRemote(exception))

  override def initiatingShutdown(): Unit =
    reporters.foreach(_.initiatingShutdown())

  override def readFromPersistentCache(persistentCacheUri: URI, marshalledLocalData: Array[Byte]): Unit =
    reporters.foreach(_.readFromPersistentCache(persistentCacheUri, marshalledLocalData))

  override def writeToPersistentCache(persistentCacheUri: URI, marshalledLocalData: Array[Byte]): Unit =
    reporters.foreach(_.writeToPersistentCache(persistentCacheUri, marshalledLocalData))
}
