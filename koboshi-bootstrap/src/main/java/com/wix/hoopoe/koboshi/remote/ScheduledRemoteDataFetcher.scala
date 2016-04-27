package com.wix.hoopoe.koboshi.remote

import java.util.concurrent.{ScheduledExecutorService, ScheduledFuture, TimeUnit}
import java.util.concurrent.atomic.AtomicBoolean

import com.wix.hoopoe.koboshi.remote.ScheduledRemoteDataFetcher._
import com.wix.hoopoe.koboshi.report.RemoteDataFetchingReporter

/**
 * @author ittaiz
 * @since 6/25/13
 */
object ScheduledRemoteDataFetcher {
  private val NoDelaySinceStaleFromDisk = 0
  private val UpToDateFetchInterval = 5
  private val StaleOrMissingFetchInterval = 1
}

class ScheduledRemoteDataFetcher[T](scheduler: ScheduledExecutorService, underlyingRemoteDataFetcher: RemoteDataFetcher[T], remoteDataFetchingReporter: RemoteDataFetchingReporter) extends RemoteDataFetcher[T] {
  private val initialized: AtomicBoolean = new AtomicBoolean
  private val scheduledRunCompletedSuccessfullyAtLeastOnce: AtomicBoolean = new AtomicBoolean
  private var initializationScheduledFuture: ScheduledFuture[_] = null

  def init(): Unit = {
    assertHasNotInitializedAlready()
    underlyingRemoteDataFetcher.init()
    initializationScheduledFuture = scheduleRun
  }

  private def scheduleRun: ScheduledFuture[_] =
    scheduler.scheduleAtFixedRate(new RunningFetcher(), initialDelay, fetchInterval, TimeUnit.MINUTES)

  private def fetchInterval: Int =
    if (hasInitializedFromRemote) UpToDateFetchInterval else StaleOrMissingFetchInterval

  private def assertHasNotInitializedAlready(): Unit =
    if (!initialized.compareAndSet(false, true)) throw new IllegalStateException("cannot init twice")

  private def initialDelay: Int =
    if (underlyingRemoteDataFetcher.hasInitializedFromDisk || !underlyingRemoteDataFetcher.hasInitializedFromRemote)
      NoDelaySinceStaleFromDisk
    else
      UpToDateFetchInterval

  def close(): Unit = {
    remoteDataFetchingReporter.initiatingShutdown()
    scheduler.shutdownNow
  }

  def fetchNow(): Unit = underlyingRemoteDataFetcher.fetchNow()

  def hasInitializedFromDisk: Boolean = underlyingRemoteDataFetcher.hasInitializedFromDisk

  def hasInitializedFromRemote: Boolean =
    underlyingRemoteDataFetcher.hasInitializedFromRemote || scheduledRunCompletedSuccessfullyAtLeastOnce.get

  private class RunningFetcher extends Runnable {
    def run(): Unit = {
      try {
        remoteDataFetchingReporter.attemptingToFetchFromRemote()
        underlyingRemoteDataFetcher.fetchNow()
        if (!initializedWithData && isFirstSuccessfulScheduledRun) {
          initializationScheduledFuture.cancel(true)
          scheduleRun
        }
      } catch {
        case e: Exception =>
          remoteDataFetchingReporter.cannotCompleteFetchingFromRemote(e)
      }
    }
  }

  private def isFirstSuccessfulScheduledRun: Boolean = scheduledRunCompletedSuccessfullyAtLeastOnce.compareAndSet(false, true)

  private def initializedWithData: Boolean = hasInitializedFromDisk || hasInitializedFromRemote
}