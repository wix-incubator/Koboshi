package com.wix.hoopoe.koboshi.cache

import com.wix.hoopoe.koboshi.cache.persistence.PersistentCaches
import com.wix.hoopoe.koboshi.marshaller.Marshallers
import com.wix.hoopoe.koboshi.namespace.NamespaceCreator._
import com.wix.hoopoe.koboshi.registry.RemoteDataFetcherRegistry
import com.wix.hoopoe.koboshi.remote.RemoteDataSource
import com.wix.hoopoe.koboshi.report.Reporters
import com.wix.hoopoe.koboshi.scheduler.{Clock, Schedulers, SchedulingDelays}

import scala.reflect.ClassTag

class CustomizableResilientCaches(remoteDataFetcherRegistry: RemoteDataFetcherRegistry,
                                  schedulers: Schedulers,
                                  reporters: Reporters,
                                  persistentCaches: PersistentCaches,
                                  marshallers: Marshallers,
                                  clock: Clock) extends ResilientCaches {

  override def aResilientInitializedCache[T](dataClass: Class[T], remoteDataSource: RemoteDataSource[T]): ReadOnlyLocalCache[T] =
    aResilientInitializedCache(remoteDataSource)(ClassTag(dataClass))

  override def aResilientInitializedCache[T : ClassTag](remoteDataSource: RemoteDataSource[T]): ReadOnlyLocalCache[T] =
    aResilientCacheBuilder(remoteDataSource).build()


  override def aResilientCacheBuilder[T](dataClass: Class[T], remoteDataSource: RemoteDataSource[T]): ResilientCacheBuilder[T, ReadOnlyLocalCache] = {
    aResilientCacheBuilder(remoteDataSource)(ClassTag(dataClass))
  }

  override def aResilientCacheBuilder[T : ClassTag](remoteDataSource: RemoteDataSource[T]): ResilientCacheBuilder[T, ReadOnlyLocalCache] =
    new BareDataResilientCacheBuilder[T](
      new TransientCacheResilientCacheBuilder[T](
        cacheNamespace,
        remoteDataSource,
        remoteDataFetcherRegistry,
        reporters,
        persistentCaches,
        schedulers,
        clock,
        marshallers,
        SchedulingDelays()))

  def stop(): Unit = remoteDataFetcherRegistry.foreach(_.close())
}
