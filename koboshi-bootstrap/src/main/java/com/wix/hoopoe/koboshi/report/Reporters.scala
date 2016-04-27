package com.wix.hoopoe.koboshi.report

trait Reporters {
  def aReporter(namespace: String): RemoteDataFetchingReporter
}
