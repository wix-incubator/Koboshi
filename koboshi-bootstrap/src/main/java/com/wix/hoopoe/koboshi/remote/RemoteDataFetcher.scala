package com.wix.hoopoe.koboshi.remote

trait RemoteDataFetcher[T] extends AutoCloseable {
  def fetchNow(): Unit

  def init(): Unit

  def close(): Unit

  def hasSyncedWithRemote: Boolean
}