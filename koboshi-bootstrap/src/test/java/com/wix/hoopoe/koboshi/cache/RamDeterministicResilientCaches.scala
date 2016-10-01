package com.wix.hoopoe.koboshi.cache

import com.fasterxml.jackson.databind.ObjectMapper
import com.wix.hoopoe.koboshi.cache.persistence.{PersistentCache, PersistentCaches, RamPersistentCache}
import com.wix.hoopoe.koboshi.marshaller.{JacksonMarshallers, Marshaller}
import com.wix.hoopoe.koboshi.registry.MapBasedRemoteDataFetcherRegistry
import com.wix.hoopoe.koboshi.report.{DelegatingRemoteDataFetcherReporter, FixedReporterReporters, RemoteDataFetchingReporter}
import com.wix.hoopoe.koboshi.scheduler.{DeterministicSchedulers, FakeClock}

class RamDeterministicResilientCaches(val remoteDataFetcherRegistry: MapBasedRemoteDataFetcherRegistry = new MapBasedRemoteDataFetcherRegistry,
                                  val objectMapper: ObjectMapper = new ObjectMapper,
                                  val reporter: RemoteDataFetchingReporter = new DelegatingRemoteDataFetcherReporter)
  extends CustomizableResilientCaches(remoteDataFetcherRegistry,
    new DeterministicSchedulers(),
    new FixedReporterReporters(reporter),
    new PersistentCaches(){
    //when we'll need access to it we'll probably need to introduce a map of (namespace,class[T])=>RamPersistentCache[T] and add to it every time this method is called
      override def aPersistentCache[T](namespace: String, marshaller: Marshaller[T], reporter: RemoteDataFetchingReporter): PersistentCache[T] = new RamPersistentCache[T]
    },
    new JacksonMarshallers(objectMapper),
    new FakeClock) {

}

