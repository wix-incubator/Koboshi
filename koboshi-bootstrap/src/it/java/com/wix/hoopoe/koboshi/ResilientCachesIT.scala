package com.wix.hoopoe.koboshi

import java.io.File
import java.net.URL
import java.util.concurrent.{ScheduledExecutorService, TimeUnit}

import ch.qos.logback.classic.Level
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.joda.JodaModule
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.wix.hoopoe.koboshi.RemoteDataFetchingLogDriver.aRemoteDataFetchingLogDriver
import com.wix.hoopoe.koboshi.ResilientCachesIT._
import com.wix.hoopoe.koboshi.cache._
import com.wix.hoopoe.koboshi.cache.persistence.{FolderPersistentCaches, PersistentCache}
import com.wix.hoopoe.koboshi.example.SomeDataType
import com.wix.hoopoe.koboshi.marshaller.{JacksonMarshallers, JacksonMarshaller}
import com.wix.hoopoe.koboshi.namespace.NamespaceCreator.cacheNamespace
import com.wix.hoopoe.koboshi.registry.MapBasedRemoteDataFetcherRegistry
import com.wix.hoopoe.koboshi.remote.{BlockingRemoteDataFetcher, RemoteDataSource}
import com.wix.hoopoe.koboshi.report.{FixedReporterReporters, LoggingRemoteDataFetchingReporter}
import com.wix.hoopoe.koboshi.scheduler.{FakeClock, Schedulers}
import org.hamcrest.Matchers.{containsString, is}
import org.hamcrest.{Description, Matcher, TypeSafeMatcher}
import org.jmock.lib.concurrent.DeterministicScheduler
import org.joda.time.Instant
import org.junit.Assert._
import org.slf4j.LoggerFactory
import org.specs2.mutable.{BeforeAfter, SpecificationWithJUnit}
import org.specs2.specification.Scope

import scala.reflect._


class ResilientCachesIT extends SpecificationWithJUnit {
  sequential
  "A Resilient Cache" should {
    "keep remote data in the transient cache" in new Context {
      val transientCacheDriver = aLocalInitializedCacheDriver()
      remoteServerDriver.stopResponding()
      transientCacheDriver.has(REMOTE_DATA)
    }

    "sync with remote source periodically" in new Context {
      val transientCacheDriver = aLocalInitializedCacheDriver()
      transientCacheDriver.has(REMOTE_DATA)
      val differentRemoteData: SomeDataType = new SomeDataType("DIFFERENT DATA")
      remoteServerDriver.respondWith(differentRemoteData)
      advanceTimeToNextScheduledFetch()
      transientCacheDriver.has(differentRemoteData)
    }

    "retrieve data from disk on init" in new Context {
      persistentCacheDriver.setTo(INITIAL_LOCAL_DATA)
      val transientCacheDriver = aLocalInitializedCacheDriver()
      transientCacheDriver.has(INITIAL_LOCAL_DATA)
    }

    "sync with remote source immediately after initializing from disk" in new Context {
      persistentCacheDriver.setTo(INITIAL_LOCAL_DATA)
      val transientCacheDriver = aLocalInitializedCacheDriver()
      transientCacheDriver.has(INITIAL_LOCAL_DATA)
      advanceTimeByANanosecond()
      transientCacheDriver.has(REMOTE_DATA)
    }
    "update the persistent cache on init if no persistent cache exists" in new Context {
      aLocalInitializedCacheDriver()
      persistentCacheDriver.has(REMOTE_DATA)
    }

    "update the persistent cache after sync with remote" in new Context {
      persistentCacheDriver.setTo(INITIAL_LOCAL_DATA)
      aLocalInitializedCacheDriver()
      advanceTimeByANanosecond()
      persistentCacheDriver.has(REMOTE_DATA)
    }

    "report failure when scheduled remote fetching fails" in new Context {
      aLocalInitializedCacheDriver()
      remoteServerDriver.stopResponding()
      advanceTimeToNextScheduledFetch()
      reportsRemoteFetchingFailureFor[SomeDataType]()
    }

    "reflect last successful data time" in new Context {
      setTimeTo(A_POINT_IN_TIME)
      val transientCacheDriver = aTimestampedLocalInitializedCacheDriver
      transientCacheDriver.has(remoteDataWithTimestamp(A_POINT_IN_TIME))
    }

    "reflect last successful data time when remote fetching fails" in new Context {
      setTimeTo(A_POINT_IN_TIME)
      val transientCacheDriver = aTimestampedLocalInitializedCacheDriver
      val aLaterPointInTime: Instant = A_POINT_IN_TIME.plus(SOME_TIME)
      setTimeTo(aLaterPointInTime)
      remoteServerDriver.stopResponding()
      advanceTimeToNextScheduledFetch()
      transientCacheDriver.has(remoteDataWithTimestamp(A_POINT_IN_TIME))
    }

    "reflect the data timestamp when loading from the persistent cache" in new Context {
      persistentCacheDriver.setTo(new TimestampedData[SomeDataType](INITIAL_LOCAL_DATA, A_POINT_IN_TIME))
      val transientCacheDriver = aTimestampedLocalInitializedCacheDriver
      transientCacheDriver.has(dataWithTimestamp(A_POINT_IN_TIME, INITIAL_LOCAL_DATA))
    }

    "persist the data timestamp to the persistent cache" in new Context {
      setTimeTo(A_POINT_IN_TIME)
      remoteServerDriver.respondWith(REMOTE_DATA)
      val transientCacheDriver = aTimestampedLocalInitializedCacheDriver
      persistentCacheDriver.has(dataWithTimestamp(A_POINT_IN_TIME, REMOTE_DATA))
    }
  }
}
trait Context extends Scope with BeforeAfter {
  val clock = new FakeClock(INITIAL_TIME)
  val remoteServerDriver = new RemoteServerDriver
  val logger = {
    LoggerFactory.getLogger(classOf[BlockingRemoteDataFetcher[SomeDataType]] + "." + classOf[SomeDataType])
  }
  val remoteDataFetchingLogDriver = aRemoteDataFetchingLogDriver(logger)
  val scheduler = new DeterministicScheduler
  val disk = better.files.File.newTemporaryDirectory()
  val persistentCacheDriver = aPersistentCacheDriver(disk.toJava)
  val resilientCaches = new CustomizableResilientCaches(new MapBasedRemoteDataFetcherRegistry,
    new FixedSchedulerSchedulers(scheduler),
    new FixedReporterReporters(new LoggingRemoteDataFetchingReporter(logger, cacheNamespace[SomeDataType])),
    new FolderPersistentCaches(disk.toJava),
    new JacksonMarshallers(aScalaAndJodaSupportingObjectMapper),
    clock)

  def before: Unit = {
    remoteServerDriver.start()
    remoteServerDriver.respondWith(REMOTE_DATA)
  }

  def after: Unit = {
    remoteServerDriver.stop()
    disk.delete(swallowIOExceptions = true)
  }

  def aLocalInitializedCacheDriver(): ReadOnlyCacheDriver[SomeDataType] =
    new ReadOnlyCacheDriver[SomeDataType](resilientCaches.aResilientInitializedCache(testDataSourceFor(remoteServerDriver)))

  def aTimestampedLocalInitializedCacheDriver: ReadOnlyTimestampedCacheDriver[SomeDataType] =
    new ReadOnlyTimestampedCacheDriver[SomeDataType](
      resilientCaches.aResilientCacheBuilder(testDataSourceFor(remoteServerDriver)).withTimestampedData().build()
    )

  def setTimeTo(time: Instant): Unit = clock.setCurrent(time)

  def advanceTimeToNextScheduledFetch(): Unit = scheduler.tick(5, TimeUnit.MINUTES)

  def advanceTimeByANanosecond(): Unit = scheduler.tick(1, TimeUnit.NANOSECONDS)

  def reportsRemoteFetchingFailureFor[T : ClassTag](): Unit =
    remoteDataFetchingLogDriver.reports(Level.ERROR, containsString(cacheNamespace(classTag[T])))
}

object ResilientCachesIT {
  val REMOTE_DATA = new SomeDataType("IRRELEVANT CONTENT")
  val INITIAL_LOCAL_DATA = new SomeDataType("CACHED DATA")
  val INITIAL_TIME = new Instant(0)
  val SOME_TIME = 60 * 1000
  val A_POINT_IN_TIME = INITIAL_TIME.plus(SOME_TIME)
  val FIVE_MINUTES = 5 * 60 * 1000

  class ReadOnlyCacheDriver[T](private val readOnlyLocalCache: ReadOnlyLocalCache[T]) {
    def has(data: T): Unit = assertThat[T](readOnlyLocalCache.read, is(data))
  }

  class PersistentCacheDriver[T](private val persistentCache: PersistentCache[T]) {
    def has(data: T): Unit = assertThat[T](persistentCache.readTimestamped.data, is(data))

    def has(matcher: Matcher[TimestampedData[T]]): Unit = assertThat(persistentCache.readTimestamped, is(matcher))

    def setTo(timestampedData: TimestampedData[T]): Unit = persistentCache.write(timestampedData)

    def setTo(data: T): Unit = setTo(new TimestampedData[T](data, new Instant))
  }

  class ReadOnlyTimestampedCacheDriver[T](private val readOnlyTimestampedLocalCache: ReadOnlyTimestampedLocalCache[T]) {
    def has(matcher: Matcher[TimestampedData[T]]): Unit = assertThat(readOnlyTimestampedLocalCache.readTimestamped, is(matcher))
  }

  def aPersistentCacheDriver(folder: File): PersistentCacheDriver[SomeDataType] = {
    val persistentCaches = new FolderPersistentCaches(folder)
    val persistentCache = persistentCaches.aPersistentCache[SomeDataType](cacheNamespace[SomeDataType], new JacksonMarshaller[SomeDataType](aScalaAndJodaSupportingObjectMapper))
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
    dataWithTimestamp(update, REMOTE_DATA)

  def dataWithTimestamp(update: Instant, data: SomeDataType): TypeSafeMatcher[TimestampedData[SomeDataType]] =
    new TypeSafeMatcher[TimestampedData[SomeDataType]]() {
      protected def matchesSafely(item: TimestampedData[SomeDataType]): Boolean = {
        is(data).matches(item.data) && is(update).matches(item.lastUpdate)
      }

      def describeTo(description: Description) {
        description.appendText("data ").appendValue(data).appendText(" and update ").appendValue(update)
      }
    }

}

private class FixedSchedulerSchedulers(scheduler: ScheduledExecutorService) extends Schedulers {
  override def aScheduler(namespace: String): ScheduledExecutorService = scheduler
}