package com.wix.hoopoe.koboshi.report

/**
 * @author ittaiz
 * @since 12/30/13
 */
class ErrorMessageGenerator(namespace: String) {
  def cannotCompleteFetchingFromRemote: String = appendNamespace("failure syncing with remote")

  def cannotReadFromPersistentCache: String = appendNamespace("failure reading persistent cache")

  def cannotWriteToPersistentCache: String = appendNamespace("failure writing to persistent cache")

  def cannotCompleteInitializingFromRemote: String = appendNamespace("failure initializing from remote")

  private def appendNamespace(message: String): String = message + " for " + namespace
}
