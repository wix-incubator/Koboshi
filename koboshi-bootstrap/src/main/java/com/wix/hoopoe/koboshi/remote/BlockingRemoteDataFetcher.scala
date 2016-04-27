package com.wix.hoopoe.koboshi.remote

import java.util.concurrent.atomic.AtomicBoolean

import com.wix.hoopoe.koboshi.cache.TimestampedData
import com.wix.hoopoe.koboshi.cache.persistence.PersistentCache
import com.wix.hoopoe.koboshi.cache.transience.TransientCache
import com.wix.hoopoe.koboshi.report.{FetchingException, RemoteDataFetchingReporter}
import com.wix.hoopoe.koboshi.scheduler.Clock

import scala.util.Try

class BlockingRemoteDataFetcher[T](remoteDataSource: RemoteDataSource[T], persistentCache: PersistentCache[T], transientCache: TransientCache[T], reporter: RemoteDataFetchingReporter, clock: Clock) extends RemoteDataFetcher[T] {
    @volatile
    private var initializedFromDisk: Boolean = false
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
      } catch {
        case e: Exception =>
          throw new FetchingException("Error fetching remote data", e)
      }
    }

    def init(): Unit = {
      assertHasNotInitializedAlready()
      val maybeFaultyMaybeEmptyPersistedData = Try(Option(persistentCache.readTimestamped))
      maybeFaultyMaybeEmptyPersistedData.failed.foreach { case e: RuntimeException =>
        reporter.cannotReadFromPersistentCache(e)
      }

      maybeFaultyMaybeEmptyPersistedData.toOption.flatten.fold {
        try {
          fetchNow()
          initializedFromRemote = true
        }
        catch {
          case e: RuntimeException =>
            reporter.cannotCompleteInitializingFromRemote(e)
        }
      } { data: TimestampedData[T] =>
        initializedFromDisk = true
        transientCache.write(data)
      }
    }

    private def assertHasNotInitializedAlready() : Unit =
        if (!initialized.compareAndSet(false, true)) throw new IllegalStateException("cannot init twice")

    private def timestampThe(remoteData: T): TimestampedData[T] = new TimestampedData[T](remoteData, clock.instant)

    def hasInitializedFromDisk: Boolean = initializedFromDisk

    def hasInitializedFromRemote: Boolean = initializedFromRemote

    def close(): Unit = {
    }
}