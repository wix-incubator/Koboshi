package com.wix.hoopoe.koboshi.report

import org.slf4j.LoggerFactory

class SLF4JReporters extends Reporters {
  override def aReporter(namespace: String): RemoteDataFetchingReporter = new LoggingRemoteDataFetchingReporter(LoggerFactory.getLogger(namespace), namespace)
}
