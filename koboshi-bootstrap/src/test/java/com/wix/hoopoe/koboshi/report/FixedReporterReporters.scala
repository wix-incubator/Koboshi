package com.wix.hoopoe.koboshi.report

class FixedReporterReporters(reporter: RemoteDataFetchingReporter) extends Reporters {
  override def aReporter(namespace: String): RemoteDataFetchingReporter = reporter
}
