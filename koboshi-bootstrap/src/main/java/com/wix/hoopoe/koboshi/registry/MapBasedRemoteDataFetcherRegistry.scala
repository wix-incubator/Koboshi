package com.wix.hoopoe.koboshi.registry

import java.util.concurrent.ConcurrentHashMap

import com.wix.hoopoe.koboshi.remote.RemoteDataFetcher

import scala.collection.JavaConverters._
import scala.collection.concurrent
/**
 * @author ittaiz
 * @since 11/7/13
 */
class MapBasedRemoteDataFetcherRegistry extends RemoteDataFetcherRegistry {
    private final val map: concurrent.Map[String, RemoteDataFetcher[_]] =
        new ConcurrentHashMap[String, RemoteDataFetcher[_]]().asScala

    def register(namespace: String, remoteDataFetcher: RemoteDataFetcher[_]) : Unit = map.put(namespace, remoteDataFetcher)

    def lookup(namespace: String): Option[RemoteDataFetcher[_]] = map.get(namespace)

    override def foreach(f: (RemoteDataFetcher[_]) => Unit): Unit = map.values.foreach(f)
}