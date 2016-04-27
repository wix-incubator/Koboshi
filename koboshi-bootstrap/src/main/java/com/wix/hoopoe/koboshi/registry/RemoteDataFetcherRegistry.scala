package com.wix.hoopoe.koboshi.registry

import com.wix.hoopoe.koboshi.remote.RemoteDataFetcher

/**
 * @author ittaiz
 * @since 11/7/13
 */
trait RemoteDataFetcherRegistry {
    def foreach(f: (RemoteDataFetcher[_]) => Unit): Unit

    def register(namespace: String, remoteDataFetcher: RemoteDataFetcher[_])

    def lookup(namespace: String): Option[RemoteDataFetcher[_]]
}