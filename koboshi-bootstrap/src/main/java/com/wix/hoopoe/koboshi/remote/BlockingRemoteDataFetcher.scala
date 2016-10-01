package com.wix.hoopoe.koboshi.remote

import java.util.concurrent.atomic.AtomicBoolean

import com.wix.hoopoe.koboshi.cache.TimestampedData
import com.wix.hoopoe.koboshi.cache.persistence.PersistentCache
import com.wix.hoopoe.koboshi.cache.transience.TransientCache
import com.wix.hoopoe.koboshi.report.{FetchingException, RemoteDataFetchingReporter}
import com.wix.hoopoe.koboshi.scheduler.Clock

import scala.util.Try

class BlockingRemoteDataFetcher[T](remoteDataSource: RemoteDataSource[T], persistentCache: PersistentCache[T], transientCache: TransientCache[T], reporter: RemoteDataFetchingReporter, clock: Clock) extends RemoteDataFetcher[T] {
  private final val initialized: AtomicBoolean = new AtomicBoolean
  @volatile
  private var initializedFromRemote: Boolean = false

  def fetchNow(): Unit = {
    //TODO move try catch to scala idiom (catching/Try) to remove noise
    try {
      val remoteData = remoteDataSource.fetch()
      val timestampedRemoteData = timestampThe(remoteData)
      transientCache.write(timestampedRemoteData)
      try {
        persistentCache.write(timestampedRemoteData)
      } catch {
        case e: RuntimeException => reporter.cannotWriteToPersistentCache(e)
      }
      initializedFromRemote = true
    } catch {
      case e: Exception =>
        throw new FetchingException("Error fetching remote data", e)
    }
  }

  def init(): Unit = {
    assertHasNotInitializedAlready()
    val maybeFaultyMaybeEmptyPersistedData = Try(Option(persistentCache.readTimestamped()))

    val maybeEmptyPersistedData = maybeFaultyMaybeEmptyPersistedData.recover { case e: RuntimeException =>
      reporter.cannotReadFromPersistentCache(e)
      None
    }.toOption.flatten

    maybeEmptyPersistedData match {
      case Some(data) => transientCache.write(data)
      case None => tryToFetchIfNoDataOrFaultyData()
    }
  }

  private def tryToFetchIfNoDataOrFaultyData(): Unit = {
    try {
      fetchNow()
    }
    catch {
      case e: RuntimeException =>
        reporter.cannotCompleteInitializingFromRemote(e)
    }
  }

  private def assertHasNotInitializedAlready(): Unit =
    if (!initialized.compareAndSet(false, true)) throw new IllegalStateException("cannot init twice")

  private def timestampThe(remoteData: T): TimestampedData[T] = new TimestampedData[T](remoteData, clock.instant)

  def close(): Unit = {
  }

  override def hasSyncedWithRemote: Boolean = initializedFromRemote
}