package com.wix.hoopoe.koboshi.report

import java.net.URI

trait RemoteDataFetchingReporter {
    def cannotCompleteFetchingFromRemote(e: Exception): Unit

    def attemptingToFetchFromRemote(): Unit /* Need to separate failures and lifecycle */

    def cannotReadFromPersistentCache(exception: RuntimeException): Unit

    def cannotWriteToPersistentCache(exception: RuntimeException): Unit

    def cannotCompleteInitializingFromRemote(exception: RuntimeException): Unit

    def initiatingShutdown(): Unit = {/* NOP, need to separate failures and lifecycle */}

    def readFromPersistentCache(persistentCacheUri: URI, marshalledLocalData: Array[Byte]): Unit = {/* NOP, need to separate failures and lifecycle */}

    def writeToPersistentCache(persistentCacheUri: URI, marshalledLocalData: Array[Byte]): Unit = {/* NOP, need to separate failures and lifecycle */}
}