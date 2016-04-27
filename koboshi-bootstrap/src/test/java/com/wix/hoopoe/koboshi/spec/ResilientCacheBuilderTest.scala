package com.wix.hoopoe.koboshi.spec

import com.wix.hoopoe.koboshi.cache.{RamDeterministicResilientCaches, ReadOnlyLocalCache}
import com.wix.hoopoe.koboshi.remote.{FakeRemoteDataSource, RemoteDataFetcher}
import com.wix.hoopoe.koboshi.report.RemoteDataFetchingReporter
import junit.framework.TestCase.fail
import org.hamcrest.CoreMatchers._
import org.hamcrest.MatcherAssert._
import org.junit.Test
import org.mockito.Mockito._

import scala.Option.empty

/**
 * @author ittaiz
 * @since 12/25/13
 */
class ResilientCacheBuilderTest {
  private val NAMESPACE = "someNamespace"
  private val remote = new FakeRemoteDataSource[String]
  private val ramResilientCaches = new RamDeterministicResilientCaches()
  @Test
  @throws(classOf[Exception])
  def disablesReadsWhenCanNotInitFromRemote() {
    remote.act(() => {
      throw new RuntimeException
    })
    val cache = ramResilientCaches.aResilientInitializedCache[String](remote)
    try {
      cache.read
      fail("System should disable reads (via exception throwing) and not fail quietly (via returning null)")
    }
    catch {
      case e: IllegalStateException => //Expecting the exception
    }
  }

  @Test def registersResilientCacheInRegistryWithNamespace() {
    aResilientCacheWithCustomNamespace(NAMESPACE)
    assertThat(ramResilientCaches.remoteDataFetcherRegistry.lookup(NAMESPACE),
      is(not(empty[RemoteDataFetcher[_]])))
  }

  //would like this test to move to ResilientCachesIT once that is broken up to an acceptance test and e2e (to acceptance part)
  @Test def shutsDownAllResilientCachesWhenTheFactoryIsStopped(): Unit = {
    val reporter = mock(classOf[RemoteDataFetchingReporter])

    val resilientCachesWithReporter = new RamDeterministicResilientCaches(reporter = reporter)

    resilientCachesWithReporter.aResilientCacheBuilder[String](remote).withCustomNamespace("one").build()
    resilientCachesWithReporter.aResilientCacheBuilder[String](remote).withCustomNamespace("two").build()

    resilientCachesWithReporter.stop()

    verify(reporter, times(2)).initiatingShutdown()
  }

  private def aResilientCacheWithCustomNamespace(namespace: String): ReadOnlyLocalCache[String] =
    ramResilientCaches.aResilientCacheBuilder[String](remote).withCustomNamespace(namespace).build()

}