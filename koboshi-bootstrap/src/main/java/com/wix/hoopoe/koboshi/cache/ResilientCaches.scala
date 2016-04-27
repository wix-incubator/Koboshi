package com.wix.hoopoe.koboshi.cache

import com.wix.hoopoe.koboshi.remote.RemoteDataSource

import scala.reflect.ClassTag

trait ResilientCaches {
    def aResilientInitializedCache[T : ClassTag](remoteDataSource: RemoteDataSource[T]): ReadOnlyLocalCache[T]

    def aResilientInitializedCache[T](dataClass: Class[T], remoteDataSource: RemoteDataSource[T]): ReadOnlyLocalCache[T]

    def aResilientCacheBuilder[T : ClassTag](remoteDataSource: RemoteDataSource[T]): ResilientCacheBuilder[T, ReadOnlyLocalCache]

    def aResilientCacheBuilder[T](dataClass: Class[T], remoteDataSource: RemoteDataSource[T]): ResilientCacheBuilder[T, ReadOnlyLocalCache]

    def stop(): Unit
}