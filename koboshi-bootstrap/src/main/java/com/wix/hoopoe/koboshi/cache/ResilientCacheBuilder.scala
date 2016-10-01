package com.wix.hoopoe.koboshi.cache

import java.util.concurrent.ScheduledExecutorService

import com.wix.hoopoe.koboshi.cache.persistence.{PersistentCache, PersistentCaches}
import com.wix.hoopoe.koboshi.cache.transience.{AtomicReferenceCache, TransientCache}
import com.wix.hoopoe.koboshi.marshaller.Marshallers
import com.wix.hoopoe.koboshi.registry.RemoteDataFetcherRegistry
import com.wix.hoopoe.koboshi.remote.{BlockingRemoteDataFetcher, RemoteDataSource, ScheduledRemoteDataFetcher}
import com.wix.hoopoe.koboshi.report.{RemoteDataFetchingReporter, Reporters}
import com.wix.hoopoe.koboshi.scheduler.{Clock, Schedulers, SchedulingDelays}

import scala.language.higherKinds
import scala.reflect.ClassTag

sealed trait ResilientCacheBuilder[T, ConcreteCache[T] <: Cache[T]] {

  def withCustomNamespace(customNamespace: String): ResilientCacheBuilder[T, ConcreteCache]

  def withCustomSchedulingDelays(customSchedulingDelays: SchedulingDelays): ResilientCacheBuilder[T, ConcreteCache]

  def withTimestampedData(): ResilientCacheBuilder[T, ReadOnlyTimestampedLocalCache]

  def build(): ConcreteCache[T]
}

private[koboshi] case class TransientCacheResilientCacheBuilder[T: ClassTag](namespace: String,
                                                                             remoteDataSource: RemoteDataSource[T],
                                                                             remoteDataFetcherRegistry: RemoteDataFetcherRegistry,
                                                                             reporters: Reporters,
                                                                             persistence: PersistentCaches,
                                                                             schedulers: Schedulers,
                                                                             clock: Clock,
                                                                             marshallers: Marshallers,
                                                                             schedulingDelays: SchedulingDelays)
  extends ResilientCacheBuilder[T, TransientCache] {

  def withCustomNamespace(customNamespace: String): ResilientCacheBuilder[T, TransientCache] =
    copy(namespace = customNamespace)

  def withCustomSchedulingDelays(customSchedulingDelays: SchedulingDelays): ResilientCacheBuilder[T, TransientCache] =
    copy(schedulingDelays = customSchedulingDelays)

  def withTimestampedData(): ResilientCacheBuilder[T, ReadOnlyTimestampedLocalCache] =
    new TimestampedResilientCacheBuilder[T](this)

  def build(): TransientCache[T] = {
    val transientCache = new InitializationAwareCache[T](new AtomicReferenceCache[T]())
    val scheduler = schedulers.aScheduler(namespace)
    val reporter = reporters.aReporter(namespace)
    val persistentCache = persistence.aPersistentCache[T](namespace, marshallers.marshaller[T], reporter)
    val remoteDataFetcher = aScheduledRemoteDataFetcher(scheduler, schedulingDelays, transientCache, reporter, persistentCache)
    remoteDataFetcherRegistry.register(namespace, remoteDataFetcher)
    remoteDataFetcher.init()
    transientCache
  }

  private def aScheduledRemoteDataFetcher(scheduler: ScheduledExecutorService,
                                          schedulingDelays: SchedulingDelays,
                                          transientCache: InitializationAwareCache[T],
                                          reporter: RemoteDataFetchingReporter,
                                          persistentCache: PersistentCache[T]): ScheduledRemoteDataFetcher[T] =
    new ScheduledRemoteDataFetcher[T](scheduler, schedulingDelays,
      new BlockingRemoteDataFetcher[T](remoteDataSource, persistentCache, transientCache, reporter, clock), reporter)
}

private[koboshi] class TimestampedResilientCacheBuilder[T](builder: ResilientCacheBuilder[T, TransientCache]) extends BaseResilientCacheBuilder[T, ReadOnlyTimestampedLocalCache](builder) {
  override protected def builderInstance(builder: ResilientCacheBuilder[T, TransientCache]): ResilientCacheBuilder[T, ReadOnlyTimestampedLocalCache] = new TimestampedResilientCacheBuilder[T](builder)
}

private[koboshi] class BareDataResilientCacheBuilder[T](builder: ResilientCacheBuilder[T, TransientCache]) extends BaseResilientCacheBuilder[T, ReadOnlyLocalCache](builder) {
  override protected def builderInstance(builder: ResilientCacheBuilder[T, TransientCache]): ResilientCacheBuilder[T, ReadOnlyLocalCache] = new BareDataResilientCacheBuilder[T](builder)
}

private[koboshi] abstract class BaseResilientCacheBuilder[T, ConcreteCache[T] >: TransientCache[T] <: Cache[T]](builder: ResilientCacheBuilder[T, TransientCache]) extends ResilientCacheBuilder[T, ConcreteCache] {

  protected def builderInstance(builder: ResilientCacheBuilder[T, TransientCache]): ResilientCacheBuilder[T, ConcreteCache]

  override def withCustomNamespace(customNamespace: String): ResilientCacheBuilder[T, ConcreteCache] =
    builderInstance(builder.withCustomNamespace(customNamespace))

  override def withCustomSchedulingDelays(customSchedulingDelays: SchedulingDelays): ResilientCacheBuilder[T, ConcreteCache] =
    builderInstance(builder.withCustomSchedulingDelays(customSchedulingDelays))

  override def withTimestampedData(): ResilientCacheBuilder[T, ReadOnlyTimestampedLocalCache] = builder.withTimestampedData()

  override def build(): ConcreteCache[T] = builder.build()
}


