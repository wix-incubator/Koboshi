package com.wix.hoopoe.koboshi.report

import java.io.File

import org.specs2.mock.Mockito
import org.specs2.mutable.SpecificationWithJUnit
import org.specs2.specification.Scope

/**
 * @author ittaiz
 * @since 12/29/13
 */
class DelegatingRemoteDataFetcherReporterTest extends SpecificationWithJUnit with Mockito {
  val anException = new RuntimeException

  class DelegatingReporterScope extends Scope {
    val oneReporter = mockAs[RemoteDataFetchingReporter]("oneReporter")
    val otherReporter = mockAs[RemoteDataFetchingReporter]("otherReporter")
    val underlyingReporters = Seq(oneReporter, otherReporter)

    val delegatingReporter = new DelegatingRemoteDataFetcherReporter(underlyingReporters: _*)
  }

  "delegating reporter" should {

    "delegate a cannotCompleteFetchingFromRemote call to its underlying reporters" in new DelegatingReporterScope {
      delegatingReporter.cannotCompleteFetchingFromRemote(anException)
      underlyingReporters.foreach(reporter => there was one(reporter).cannotCompleteFetchingFromRemote(anException))
    }

    "delegate a cannotReadFromPersistentCache call to its underlying reporters" in new DelegatingReporterScope {
      delegatingReporter.cannotReadFromPersistentCache(anException)
      underlyingReporters.foreach(reporter => there was one(reporter).cannotReadFromPersistentCache(anException))
    }

    "delegate a cannotWriteToPersistentCache call to its underlying reporters" in new DelegatingReporterScope {
      delegatingReporter.cannotWriteToPersistentCache(anException)
      underlyingReporters.foreach(reporter => there was one(reporter).cannotWriteToPersistentCache(anException))
    }

    "delegate a attemptingToFetchFromRemote call to its underlying reporters" in new DelegatingReporterScope {
      delegatingReporter.attemptingToFetchFromRemote()
      underlyingReporters.foreach(reporter => there was one(reporter).attemptingToFetchFromRemote())
    }

    "delegate a cannotCompleteInitializingFromRemote call to its underlying reporters" in new DelegatingReporterScope {
      delegatingReporter.cannotCompleteInitializingFromRemote(anException)
      underlyingReporters.foreach(reporter => there was one(reporter).cannotCompleteInitializingFromRemote(anException))
    }

    "delegate an initiatingShutdown call to its underlying reporters" in new DelegatingReporterScope {
      delegatingReporter.initiatingShutdown()
      underlyingReporters.foreach(reporter => there was one(reporter).initiatingShutdown())
    }

    "delegate a readFromPersistentCache call to its underlying reporters" in new DelegatingReporterScope with CacheReporting {
      delegatingReporter.readFromPersistentCache(CACHE_URI, CONTENT)

      underlyingReporters.foreach(reporter => there was one(reporter).readFromPersistentCache(CACHE_URI, CONTENT))
    }

    "delegate a writeToPersistentCache call to its underlying reporters" in new DelegatingReporterScope with CacheReporting {
      delegatingReporter.writeToPersistentCache(CACHE_URI, CONTENT)

      underlyingReporters.foreach(reporter => there was one(reporter).writeToPersistentCache(CACHE_URI, CONTENT))
    }

  }

  trait CacheReporting {
      val CONTENT = "CONTENT".getBytes
      val CACHE_URI = new File("IRRELEVANT").toURI
  }

}
