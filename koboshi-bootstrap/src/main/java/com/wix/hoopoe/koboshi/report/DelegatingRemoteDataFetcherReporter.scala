package com.wix.hoopoe.koboshi.report

/**
 * @author ittaiz
 * @since 12/29/13
 */
class DelegatingRemoteDataFetcherReporter(reporters: RemoteDataFetchingReporter*) extends RemoteDataFetchingReporter {
  def cannotCompleteFetchingFromRemote(e: Exception) = reporters.foreach(_.cannotCompleteFetchingFromRemote(e))

  def attemptingToFetchFromRemote() = reporters.foreach(_.attemptingToFetchFromRemote())

  def cannotReadFromPersistentCache(exception: RuntimeException) = reporters.foreach(_.cannotReadFromPersistentCache(exception))

  def cannotWriteToPersistentCache(exception: RuntimeException) = reporters.foreach(_.cannotWriteToPersistentCache(exception))

  def cannotCompleteInitializingFromRemote(exception: RuntimeException) = reporters.foreach(_.cannotCompleteInitializingFromRemote(exception))

  override def initiatingShutdown(): Unit = reporters.foreach(_.initiatingShutdown())
}
