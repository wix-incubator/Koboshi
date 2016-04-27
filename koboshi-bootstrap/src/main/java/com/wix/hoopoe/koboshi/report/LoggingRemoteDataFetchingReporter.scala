package com.wix.hoopoe.koboshi.report

import org.slf4j.Logger

class LoggingRemoteDataFetchingReporter(logger: Logger, namespace: String) extends RemoteDataFetchingReporter {
  private final val errorMessageGenerator: ErrorMessageGenerator = new ErrorMessageGenerator(namespace)

  override def cannotCompleteFetchingFromRemote(e: Exception): Unit = {
    logger.error(errorMessageGenerator.cannotCompleteFetchingFromRemote, e)
  }

  override def attemptingToFetchFromRemote(): Unit = {
    if (logger.isTraceEnabled) {
      logger.trace("attempting to sync with remote for " + namespace)
    }
  }

  override def cannotReadFromPersistentCache(exception: RuntimeException): Unit = {
    logger.warn(errorMessageGenerator.cannotReadFromPersistentCache, exception)
  }

  override def cannotWriteToPersistentCache(exception: RuntimeException): Unit = {
    logger.warn(errorMessageGenerator.cannotWriteToPersistentCache, exception)
  }

  override def cannotCompleteInitializingFromRemote(exception: RuntimeException): Unit = {
    logger.error(errorMessageGenerator.cannotCompleteInitializingFromRemote, exception)
  }

  override def initiatingShutdown(): Unit = {
    logger.info("initiating shutdown for " + namespace)
  }
}