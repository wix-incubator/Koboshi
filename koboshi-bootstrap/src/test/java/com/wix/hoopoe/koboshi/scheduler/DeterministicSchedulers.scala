package com.wix.hoopoe.koboshi.scheduler

import java.util

import org.jmock.lib.concurrent.DeterministicScheduler

/**
 * @author ittaiz
 * @since 8/2/14
 */
//https://github.com/jmock-developers/jmock-library/issues/62
private class ShutdownAllowingDeterministicScheduler extends DeterministicScheduler {
  override def isShutdown: Boolean = shutdownRequested

  override def isTerminated: Boolean = shutdownRequested

  override def shutdown(): Unit = shutdownRequested = true

  override def shutdownNow: util.List[Runnable] = {
    shutdown()
    new util.ArrayList[Runnable]
  }

  var shutdownRequested: Boolean = false
}

class DeterministicSchedulers extends Schedulers {
  override def aScheduler(namespace: String): DeterministicScheduler =
    new ShutdownAllowingDeterministicScheduler
}