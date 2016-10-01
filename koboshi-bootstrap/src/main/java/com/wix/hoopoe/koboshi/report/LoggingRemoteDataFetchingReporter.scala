package com.wix.hoopoe.koboshi.report

import java.net.URI

import org.slf4j.Logger

class LoggingRemoteDataFetchingReporter(logger: Logger, namespace: String) extends RemoteDataFetchingReporter {
  private final val errorMessageGenerator: ErrorMessageGenerator = new ErrorMessageGenerator(namespace)

  override def cannotCompleteFetchingFromRemote(e: Exception): Unit = {
    logger.error(errorMessageGenerator.cannotCompleteFetchingFromRemote, e)
  }

  override def attemptingToFetchFromRemote(): Unit =
    traceIfEnabled {
      s"attempting to sync with remote for $namespace"
    }

  override def cannotReadFromPersistentCache(exception: RuntimeException): Unit =
    logger.warn(errorMessageGenerator.cannotReadFromPersistentCache, exception)

  override def cannotWriteToPersistentCache(exception: RuntimeException): Unit =
    logger.warn(errorMessageGenerator.cannotWriteToPersistentCache, exception)

  override def cannotCompleteInitializingFromRemote(exception: RuntimeException): Unit =
    logger.error(errorMessageGenerator.cannotCompleteInitializingFromRemote, exception)

  override def initiatingShutdown(): Unit =
    logger.info("initiating shutdown for " + namespace)

  override def readFromPersistentCache(persistentCacheUri: URI, marshalledLocalData: Array[Byte]): Unit =
    traceIfEnabled {
      s"read ${marshalledLocalData.length} bytes using $persistentCacheUri"
    }

  override def writeToPersistentCache(persistentCacheUri: URI, marshalledLocalData: Array[Byte]): Unit =
    traceIfEnabled {
      s"writing ${marshalledLocalData.length} bytes using $persistentCacheUri"
    }

  private def traceIfEnabled(msg: => String) : Unit =
    if (logger.isTraceEnabled) {
      logger.trace(msg)
    }
}