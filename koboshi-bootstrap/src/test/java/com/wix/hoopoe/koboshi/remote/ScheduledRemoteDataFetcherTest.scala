package com.wix.hoopoe.koboshi.remote

import java.util.concurrent.TimeUnit

import com.wix.hoopoe.koboshi.example.SomeDataType
import com.wix.hoopoe.koboshi.report.RemoteDataFetchingReporter
import com.wix.hoopoe.koboshi.scheduler.{DeterministicSchedulers, SchedulingDelays}
import org.jmock.lib.concurrent.DeterministicScheduler
import org.specs2.matcher.Matcher
import org.specs2.mock.Mockito
import org.specs2.mutable.SpecWithJUnit
import org.specs2.specification.Scope

class ScheduledRemoteDataFetcherTest extends SpecWithJUnit with Mockito {

  "ScheduledRemoteDataFetcher" should {
    "error when init occurs more than once" in new Context {
      scheduledRemoteDataFetcher.init()
      scheduledRemoteDataFetcher.init() must throwAn[IllegalStateException]
    }

    "fetch on demand" in new Context {
      scheduledRemoteDataFetcher.fetchNow()

      there was one(remoteDataFetcher).fetchNow()
    }

    "report error fetching from remote" in new Context {
      val e = new RuntimeException("IRRELEVANT")
      remoteDataFetcher.fetchNow() throws e

      scheduledRemoteDataFetcher.init()
      advanceTimeAfterFetch()

      there was atLeastOne(remoteDataFetchingReporter).cannotCompleteFetchingFromRemote(e)
    }

    "report remote fetching attempt" in new Context {
      scheduledRemoteDataFetcher.init()
      advanceTimeAfterFetch()

      there was atLeastOne(remoteDataFetchingReporter).attemptingToFetchFromRemote()
    }

    "shut down the scheduled executor service on stop" in new Context {
      scheduledRemoteDataFetcher.init()

      scheduledRemoteDataFetcher.close()

      scheduler must beShutdown()

    }
  }

  trait Context extends Scope {
    val scheduler = new DeterministicSchedulers().aScheduler("IRRELEVANT NAMESPACE")
    val remoteDataFetcher = mock[RemoteDataFetcher[SomeDataType]]
    val remoteDataFetchingReporter = mock[RemoteDataFetchingReporter]
    val schedulingDelays = SchedulingDelays()
    val scheduledRemoteDataFetcher = new ScheduledRemoteDataFetcher[SomeDataType](scheduler, schedulingDelays, remoteDataFetcher, remoteDataFetchingReporter)

    def advanceTimeAfterFetch(): Unit = scheduler.tick(schedulingDelays.initialDelayWhenHaveNotSyncedWithRemote.toMillis, TimeUnit.MILLISECONDS)
  }

  def beShutdown(): Matcher[DeterministicScheduler] = beTrue ^^ { s: DeterministicScheduler => s.isShutdown aka "isShutdown" }
}
