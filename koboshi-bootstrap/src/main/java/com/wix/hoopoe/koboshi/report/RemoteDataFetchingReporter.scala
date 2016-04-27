package com.wix.hoopoe.koboshi.report

trait RemoteDataFetchingReporter {
    def cannotCompleteFetchingFromRemote(e: Exception): Unit

    def attemptingToFetchFromRemote(): Unit

    def cannotReadFromPersistentCache(exception: RuntimeException): Unit

    def cannotWriteToPersistentCache(exception: RuntimeException): Unit

    def cannotCompleteInitializingFromRemote(exception: RuntimeException): Unit

    def initiatingShutdown(): Unit = {/*NOP to keep backward compatibility with app store*/}
}