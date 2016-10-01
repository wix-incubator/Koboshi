package com.wix.hoopoe.koboshi

import java.io.File
import java.net.URL
import java.util.concurrent.{ScheduledExecutorService, TimeUnit}

import better.files.{File => BetterFile}
import ch.qos.logback.classic.Level
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.joda.JodaModule
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.wix.hoopoe.koboshi.RemoteDataFetchingLogDriver.aRemoteDataFetchingLogDriver
import com.wix.hoopoe.koboshi.ResilientCachesIT._
import com.wix.hoopoe.koboshi.SchedulingContext.SchedulingAugmentor
import com.wix.hoopoe.koboshi.cache.persistence.{FolderPersistentCaches, PersistentCache}
import com.wix.hoopoe.koboshi.cache.{ReadOnlyLocalCache, ResilientCacheBuilder, _}
import com.wix.hoopoe.koboshi.example.SomeDataType
import com.wix.hoopoe.koboshi.marshaller.{JacksonMarshaller, JacksonMarshallers}
import com.wix.hoopoe.koboshi.namespace.NamespaceCreator.cacheNamespace
import com.wix.hoopoe.koboshi.registry.MapBasedRemoteDataFetcherRegistry
import com.wix.hoopoe.koboshi.remote.{BlockingRemoteDataFetcher, RemoteDataSource}
import com.wix.hoopoe.koboshi.report.{DelegatingRemoteDataFetcherReporter, FixedReporterReporters, LoggingRemoteDataFetchingReporter}
import com.wix.hoopoe.koboshi.scheduler.{FakeClock, Schedulers, SchedulingDelays}
import org.hamcrest.Matchers.{containsString, is}
import org.hamcrest.{Description, Matcher, TypeSafeMatcher}
import org.jmock.lib.concurrent.DeterministicScheduler
import org.joda.time.Instant
import org.junit.Assert._
import org.slf4j.LoggerFactory
import org.specs2.DescribeOf
import org.specs2.mutable.{BeforeAfter, SpecificationWithJUnit}
import org.specs2.specification.Scope

import scala.concurrent.duration.{Duration, _}
import scala.reflect._


class ResilientCachesIT extends SpecificationWithJUnit {
  sequential
  "A Resilient Cache" should {
    "keep remote data available" in new Context {
      val transientCacheDriver = aCacheInitializedFromRemoteWith(RemoteData)
      remoteServerDriver.stopResponding()
      transientCacheDriver.has(RemoteData)
    }

    "sync with remote source periodically" in new Context {
      val transientCacheDriver = aCacheInitializedFromRemoteWith(RemoteData)
      transientCacheDriver.has(RemoteData)
      val differentRemoteData: SomeDataType = new SomeDataType("DIFFERENT DATA")
      remoteServerDriver.respondWith(differentRemoteData)
      advanceTimeToNextScheduledFetch()
      transientCacheDriver.has(differentRemoteData)
    }

    "retrieve data from disk on init" in new Context {
      persistentCacheDriver.setTo(InitialLocalData)
      val transientCacheDriver = aLocalInitializedCacheDriver()
      transientCacheDriver.has(InitialLocalData)
    }

    "update the persistent cache on init if no persistent cache exists" in new Context {
      aCacheInitializedFromRemoteWith(RemoteData)
      persistentCacheDriver.has(RemoteData)
    }

    "update the persistent cache after sync with remote" in new Context {
      persistentCacheDriver.setTo(InitialLocalData)
      aCacheInitializedFromRemoteWith(RemoteData)
      advanceTimeBy(defaultSchedulingDelays.initialDelayWhenHaveNotSyncedWithRemote)
      persistentCacheDriver.has(RemoteData)
    }

    "report failure when scheduled remote fetching fails" in new Context with LogReporting {
      aLocalInitializedCacheDriver()
      remoteServerDriver.stopResponding()
      advanceTimeToNextScheduledFetch()
      reportsRemoteFetchingFailureFor[SomeDataType]()
    }

    "reflect last successful data time" in new Context {
      setTimeTo(APointInTime)
      val transientCacheDriver = aTimestampedCacheInitializedFromRemote()
      transientCacheDriver.has(remoteDataWithTimestamp(APointInTime))
    }

    "reflect last successful data time when remote fetching fails" in new Context {
      setTimeTo(APointInTime)
      val transientCacheDriver = aTimestampedCacheInitializedFromRemote()
      val aLaterPointInTime: Instant = APointInTime.plus(SomeTime)
      setTimeTo(aLaterPointInTime)
      remoteServerDriver.stopResponding()
      advanceTimeToNextScheduledFetch()
      transientCacheDriver.has(remoteDataWithTimestamp(APointInTime))
    }

    "reflect the data timestamp when loading from the persistent cache" in new Context {
      persistentCacheDriver.setTo(new TimestampedData[SomeDataType](InitialLocalData, APointInTime))
      val transientCacheDriver = aTimestampedLocalInitializedCacheDriver()
      transientCacheDriver.has(dataWithTimestamp(APointInTime, InitialLocalData))
    }

    "persist the data timestamp to the persistent cache" in new Context {
      remoteServerDriver.respondWith(RemoteData)
      setTimeTo(APointInTime)
      val transientCacheDriver = aTimestampedLocalInitializedCacheDriver()
      persistentCacheDriver.has(dataWithTimestamp(APointInTime, RemoteData))
    }

    "report persistent cache activities" in new Context with LogReporting {
      val transientCacheDriver = aLocalInitializedCacheDriver()
      remoteDataFetchingLogDriver.reports(Level.TRACE, containsString(disk.toJava.getPath))
    }
  }

}

trait SchedulingContractTest extends SpecificationWithJUnit with DescribeOf {
  sequential

  val schedulingDelays: SchedulingDelays

  implicit def augmentor: SchedulingAugmentor

  import schedulingDelays._

  "A Resilient Cache's scheduling" should {

    "have a first sync delay" of {
      s"$initialDelayWhenHaveNotSyncedWithRemote when initialized from persistent cache" in new SchedulingContext {
        val transientCacheDriver = aCacheInitializedFromPersistentCopy()

        remoteServerDriver.respondWith(RemoteData)
        advanceTimeBy(initialDelayWhenHaveNotSyncedWithRemote)
        transientCacheDriver.has(RemoteData)
      }

      s"$initialDelayWhenHaveNotSyncedWithRemote when initialized without data" in new SchedulingContext {
        val transientCacheDriver = aCacheInitializedWithoutData()

        remoteServerDriver.respondWith(RemoteData)
        advanceTimeBy(initialDelayWhenHaveNotSyncedWithRemote)
        transientCacheDriver.has(RemoteData)
      }

      s"$initialFetchDelayWhenSyncedWithRemote when initialized from remote" in new SchedulingContext {
        val transientCacheDriver = aCacheInitializedFromRemoteWith(RemoteData)

        remoteServerDriver.respondWith(DifferentData)
        private val smallTime = initialFetchDelayWhenSyncedWithRemote * epsilon
        advanceTimeBy(initialFetchDelayWhenSyncedWithRemote - smallTime)
        transientCacheDriver.has(RemoteData)

        advanceTimeBy(smallTime)
        transientCacheDriver.has(DifferentData)
      }

    }
    //do the -1 trick for the tests that don't already do it

    "have an ongoing sync delay" of {
      s"$nextFetchDelayWhenHaveNotSyncedWithRemote when initialized from persistent cache and initial sync failed" in new SchedulingContext {
        val transientCacheDriver = aCacheInitializedFromPersistentCopy()

        initialSyncFails(schedulingDelays)

        remoteServerDriver.respondWith(DifferentData)
        advanceTimeBy(nextFetchDelayWhenHaveNotSyncedWithRemote)
        transientCacheDriver.has(DifferentData)
      }


      s"$nextFetchDelayWhenSyncedWithRemote when initialized from persistent cache and initial sync succeeds" in new SchedulingContext {
        val transientCacheDriver = aCacheInitializedFromPersistentCopy()

        initialSyncSucceedsWith(DifferentData, schedulingDelays)

        remoteServerDriver.respondWith(YetAnotherDifferentData)
        private val smallTime = nextFetchDelayWhenSyncedWithRemote * epsilon
        advanceTimeBy(nextFetchDelayWhenSyncedWithRemote - smallTime)
        transientCacheDriver.has(DifferentData)

        advanceTimeBy(smallTime)
        transientCacheDriver.has(YetAnotherDifferentData)
      }


      s"$nextFetchDelayWhenHaveNotSyncedWithRemote when initialized without data and initial sync failed" in new SchedulingContext {
        val transientCacheDriver = aCacheInitializedWithoutData()

        initialSyncFails(schedulingDelays)

        remoteServerDriver.respondWith(DifferentData)
        advanceTimeBy(nextFetchDelayWhenHaveNotSyncedWithRemote)
        transientCacheDriver.has(DifferentData)
      }

      s"$nextFetchDelayWhenSyncedWithRemote when initialized without data and initial sync succeeds" in new SchedulingContext {
        val transientCacheDriver = aCacheInitializedWithoutData()

        initialSyncSucceedsWith(DifferentData, schedulingDelays)

        remoteServerDriver.respondWith(YetAnotherDifferentData)
        private val smallTime = nextFetchDelayWhenSyncedWithRemote * epsilon
        advanceTimeBy(nextFetchDelayWhenSyncedWithRemote - smallTime)
        transientCacheDriver.has(DifferentData)

        advanceTimeBy(smallTime)
        transientCacheDriver.has(YetAnotherDifferentData)
      }

      s"$nextFetchDelayWhenSyncedWithRemote when initialized from remote and initial sync succeeds" in new SchedulingContext {
        val transientCacheDriver = aCacheInitializedFromRemote()

        initialSyncSucceedsWith(DifferentData, schedulingDelays)

        remoteServerDriver.respondWith(YetAnotherDifferentData)
        private val smallTime = nextFetchDelayWhenSyncedWithRemote * epsilon
        advanceTimeBy(nextFetchDelayWhenSyncedWithRemote - smallTime)
        transientCacheDriver.has(DifferentData)

        advanceTimeBy(smallTime)
        transientCacheDriver.has(YetAnotherDifferentData)

        override def initialSyncSucceedsWith(data: SomeDataType, delays: SchedulingDelays): Unit = {
          remoteServerDriver.respondWith(data)
          advanceTimeBy(delays.initialFetchDelayWhenSyncedWithRemote)
        }
      }

    }
  }
}

class DefaultSchedulingTest extends SchedulingContractTest {
  val schedulingDelays: SchedulingDelays = SchedulingDelays.Default

  override implicit def augmentor: SchedulingAugmentor = SchedulingAugmentor.identity
}

class CustomSchedulingTest extends SchedulingContractTest {
  val schedulingDelays: SchedulingDelays = SchedulingDelays(
    initialFetchDelayWhenSyncedWithRemote = 10.minutes,
    nextFetchDelayWhenSyncedWithRemote = 10.minutes,
    nextFetchDelayWhenHaveNotSyncedWithRemote = 3.minutes
  )

  override implicit def augmentor: SchedulingAugmentor = SchedulingAugmentor(_.withCustomSchedulingDelays(schedulingDelays))
}

abstract class SchedulingContext(implicit augmentor: SchedulingAugmentor) extends Context {

  override def aLocalInitializedCacheDriver(): ReadOnlyCacheDriver[SomeDataType] = {
    val scheduledBuilder = augmentor.augment(aTestResilientCacheBuilder)
    new ReadOnlyCacheDriver[SomeDataType](scheduledBuilder.build())
  }
}

object SchedulingContext {

  type CacheBuilder = ResilientCacheBuilder[SomeDataType, ReadOnlyLocalCache]

  trait SchedulingAugmentor {

    def augment(builder: CacheBuilder): CacheBuilder
  }

  object SchedulingAugmentor {

    def identity: SchedulingAugmentor = SchedulingAugmentor(Predef.identity)

    def apply(f: CacheBuilder => CacheBuilder): SchedulingAugmentor = new SchedulingAugmentor {
      override def augment(builder: CacheBuilder): CacheBuilder = f(builder)
    }
  }
}

trait Context extends Scope with BeforeAfter with Log {
  val defaultSchedulingDelays = SchedulingDelays()

  def initialSyncFails(delays: SchedulingDelays): Unit = {
    remoteServerDriver.stopResponding()
    advanceTimeBy(delays.initialDelayWhenHaveNotSyncedWithRemote)
  }

  def initialSyncSucceedsWith(data: SomeDataType, delays: SchedulingDelays): Unit = {
    remoteServerDriver.respondWith(data)
    advanceTimeBy(delays.initialDelayWhenHaveNotSyncedWithRemote)
  }

  def aCacheInitializedFromRemote(): ReadOnlyCacheDriver[SomeDataType] =
    aCacheInitializedFromRemoteWith(RemoteData)

  def aCacheInitializedFromRemoteWith(data: SomeDataType): ReadOnlyCacheDriver[SomeDataType] = {
    remoteServerDriver.respondWith(data)
    aLocalInitializedCacheDriver()
  }

  def aTimestampedCacheInitializedFromRemote(): ReadOnlyTimestampedCacheDriver[SomeDataType] = {
    remoteServerDriver.respondWith(RemoteData)
    aTimestampedLocalInitializedCacheDriver()
  }

  def aCacheInitializedWithoutData(): ReadOnlyCacheDriver[SomeDataType] = {
    remoteServerDriver.stopResponding()
    //the persistent cache is empty by default
    aLocalInitializedCacheDriver()
  }

  def aCacheInitializedFromPersistentCopy(): ReadOnlyCacheDriver[SomeDataType] = {
    persistentCacheDriver.setTo(InitialLocalData)
    aLocalInitializedCacheDriver()
  }

  val clock = new FakeClock(InitialTime)
  val remoteServerDriver = new RemoteServerDriver
  val scheduler = new DeterministicScheduler
  val disk = BetterFile.newTemporaryDirectory(prefix = "koboshi")
  val persistentCacheDriver = aPersistentCacheDriver(disk.toJava)
  val resilientCaches = new CustomizableResilientCaches(new MapBasedRemoteDataFetcherRegistry,
    new FixedSchedulerSchedulers(scheduler),
    new FixedReporterReporters(new LoggingRemoteDataFetchingReporter(logger, cacheNamespace[SomeDataType])),
    new FolderPersistentCaches(disk.toJava),
    new JacksonMarshallers(aScalaAndJodaSupportingObjectMapper),
    clock)

  def before: Unit = {
    deletePersistentCacheLeftovers(disk)
    remoteServerDriver.start()
  }

  def after: Unit = {
    remoteServerDriver.stop()
  }

  def aLocalInitializedCacheDriver(): ReadOnlyCacheDriver[SomeDataType] =
    new ReadOnlyCacheDriver[SomeDataType](resilientCaches.aResilientInitializedCache(testDataSourceFor(remoteServerDriver)))

  def aTimestampedLocalInitializedCacheDriver(): ReadOnlyTimestampedCacheDriver[SomeDataType] =
    new ReadOnlyTimestampedCacheDriver[SomeDataType](aTestResilientCacheBuilder.withTimestampedData().build())

  def aTestResilientCacheBuilder: ResilientCacheBuilder[SomeDataType, ReadOnlyLocalCache] =
    resilientCaches.aResilientCacheBuilder(testDataSourceFor(remoteServerDriver))

  def setTimeTo(time: Instant): Unit = clock.setCurrent(time)

  def advanceTimeToNextScheduledFetch(): Unit = advanceTimeBy(defaultSchedulingDelays.nextFetchDelayWhenSyncedWithRemote)

  def advanceTimeBy(duration: Duration): Unit = scheduler.tick(duration.toMillis, TimeUnit.MILLISECONDS)
}

trait Log {
  val logger = LoggerFactory.getLogger(classOf[BlockingRemoteDataFetcher[SomeDataType]] + "." + classOf[SomeDataType])
}

trait LogReporting extends Log {
  val remoteDataFetchingLogDriver = aRemoteDataFetchingLogDriver(logger)

  def reportsRemoteFetchingFailureFor[T: ClassTag](): Unit =
    remoteDataFetchingLogDriver.reports(Level.ERROR, containsString(cacheNamespace(classTag[T])))
}

object ResilientCachesIT {
  val RemoteData = new SomeDataType("IRRELEVANT CONTENT")
  val DifferentData = new SomeDataType("DIFFERENT DATA")
  val YetAnotherDifferentData = new SomeDataType("YET ANOTHER DIFFERENT DATA")
  val InitialLocalData = new SomeDataType("CACHED DATA")
  val epsilon = 0.1
  val InitialTime = new Instant(0)
  val SomeTime = 60 * 1000
  val APointInTime = InitialTime.plus(SomeTime)

  class ReadOnlyCacheDriver[T](private val readOnlyLocalCache: ReadOnlyLocalCache[T]) {
    def has(data: T): Unit = assertThat[T](readOnlyLocalCache.read(), is(data))
  }

  class PersistentCacheDriver[T](private val persistentCache: PersistentCache[T]) {
    def has(data: T): Unit = assertThat[T](persistentCache.readTimestamped().data, is(data))

    def has(matcher: Matcher[TimestampedData[T]]): Unit = assertThat(persistentCache.readTimestamped(), is(matcher))

    def setTo(timestampedData: TimestampedData[T]): Unit = persistentCache.write(timestampedData)

    def setTo(data: T): Unit = setTo(new TimestampedData[T](data, new Instant))
  }

  class ReadOnlyTimestampedCacheDriver[T](private val readOnlyTimestampedLocalCache: ReadOnlyTimestampedLocalCache[T]) {
    def has(matcher: Matcher[TimestampedData[T]]): Unit = assertThat(readOnlyTimestampedLocalCache.readTimestamped(), is(matcher))
  }

  def aPersistentCacheDriver(folder: File): PersistentCacheDriver[SomeDataType] = {
    val persistentCaches = new FolderPersistentCaches(folder)
    val persistentCache = persistentCaches.aPersistentCache[SomeDataType](cacheNamespace[SomeDataType], new JacksonMarshaller[SomeDataType](aScalaAndJodaSupportingObjectMapper), new DelegatingRemoteDataFetcherReporter())
    new PersistentCacheDriver[SomeDataType](persistentCache)
  }

  def aScalaAndJodaSupportingObjectMapper: ObjectMapper = new ObjectMapper().registerModule(DefaultScalaModule).registerModule(new JodaModule)

  def testDataSourceFor(remoteServerDriver: RemoteServerDriver): RemoteDataSource[SomeDataType] =
    new RemoteDataSource[SomeDataType]() {
      val remoteUrl = new URL("http://localhost:" + remoteServerDriver.port)
      val objectMapper = aScalaAndJodaSupportingObjectMapper

      def fetch(): SomeDataType = objectMapper.readValue(remoteUrl, classOf[SomeDataType])
    }

  def remoteDataWithTimestamp(update: Instant): Matcher[TimestampedData[SomeDataType]] =
    dataWithTimestamp(update, RemoteData)

  def dataWithTimestamp(update: Instant, data: SomeDataType): TypeSafeMatcher[TimestampedData[SomeDataType]] =
    new TypeSafeMatcher[TimestampedData[SomeDataType]]() {
      protected def matchesSafely(item: TimestampedData[SomeDataType]): Boolean = {
        is(data).matches(item.data) && is(update).matches(item.lastUpdate)
      }

      def describeTo(description: Description) {
        description.appendText("data ").appendValue(data).appendText(" and update ").appendValue(update)
      }
    }

  def deletePersistentCacheLeftovers(persistentCacheFolder: better.files.File): Unit = {
    persistentCacheFolder.parent
      .collectChildren(isKoboshiTempDir)
      .filter(notTheCurrentTestTempDir)
      .foreach(f => f.delete(swallowIOExceptions = true))

    def isKoboshiTempDir(f: BetterFile) = f.name.startsWith("koboshi")
    def notTheCurrentTestTempDir(f: BetterFile) = f != persistentCacheFolder
  }
}

private class FixedSchedulerSchedulers(scheduler: ScheduledExecutorService) extends Schedulers {
  override def aScheduler(namespace: String): ScheduledExecutorService = scheduler
}