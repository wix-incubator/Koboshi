package com.wix.hoopoe.koboshi.remote

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.{ScheduledExecutorService, ScheduledFuture, TimeUnit}

import com.wix.hoopoe.koboshi.report.RemoteDataFetchingReporter
import com.wix.hoopoe.koboshi.scheduler.SchedulingDelays

import scala.concurrent.duration.FiniteDuration

/**
  * @author ittaiz
  * @since 6/25/13
  */
class ScheduledRemoteDataFetcher[T](scheduler: ScheduledExecutorService,
                                    schedulingDelays: SchedulingDelays,
                                    underlyingRemoteDataFetcher: RemoteDataFetcher[T],
                                    remoteDataFetchingReporter: RemoteDataFetchingReporter) extends RemoteDataFetcher[T] {
  private val initialized = new AtomicBoolean

  def init(): Unit = {
    assertHasNotInitializedAlready()
    underlyingRemoteDataFetcher.init()
    scheduleFirstRun()
  }

  private def assertHasNotInitializedAlready(): Unit =
    if (!initialized.compareAndSet(false, true)) throw new IllegalStateException("cannot init twice")

  private def scheduleFirstRun(): ScheduledFuture[_] =
    scheduleWithDelay(schedulingDelays.initialDelay(hasSyncedWithRemote))

  private def scheduleNextRun(): ScheduledFuture[_] =
    scheduleWithDelay(schedulingDelays.nextFetchDelay(hasSyncedWithRemote))

  private def scheduleWithDelay(delay: FiniteDuration): ScheduledFuture[_] =
    scheduler.schedule(RunningFetcher, delay.toMillis, TimeUnit.MILLISECONDS)

  def close(): Unit = {
    remoteDataFetchingReporter.initiatingShutdown()
    scheduler.shutdownNow()
  }

  def fetchNow(): Unit = underlyingRemoteDataFetcher.fetchNow()

  def hasSyncedWithRemote: Boolean = underlyingRemoteDataFetcher.hasSyncedWithRemote

  private object RunningFetcher extends Runnable {
    def run(): Unit = {
      try {
        remoteDataFetchingReporter.attemptingToFetchFromRemote()
        underlyingRemoteDataFetcher.fetchNow()
      } catch {
        case e: Exception =>
          remoteDataFetchingReporter.cannotCompleteFetchingFromRemote(e)
      } finally {
        scheduleNextRun()
      }
    }
  }

}