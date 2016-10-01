package com.wix.hoopoe.koboshi.scheduler

import com.wix.hoopoe.koboshi.scheduler.SchedulingDelays._

import scala.concurrent.duration._

object SchedulingDelays {
  private val StaleOrMissingInitialDelay = 1.millisecond
  private val UpToDateFetchInterval = 60.seconds
  private val StaleOrMissingFetchInterval = 10.seconds
  val Default = SchedulingDelays()
}

case class SchedulingDelays(
                             initialFetchDelayWhenSyncedWithRemote: FiniteDuration = UpToDateFetchInterval,
                             initialDelayWhenHaveNotSyncedWithRemote: FiniteDuration = StaleOrMissingInitialDelay,
                             nextFetchDelayWhenSyncedWithRemote: FiniteDuration = UpToDateFetchInterval,
                             nextFetchDelayWhenHaveNotSyncedWithRemote: FiniteDuration = StaleOrMissingFetchInterval
                           ) {
  def nextFetchDelay(hasSyncedWithRemote: Boolean): FiniteDuration =
    if (hasSyncedWithRemote)
      nextFetchDelayWhenSyncedWithRemote
    else
      nextFetchDelayWhenHaveNotSyncedWithRemote

  def initialDelay(hasSyncedWithRemote: Boolean): FiniteDuration =
    if (hasSyncedWithRemote)
      initialFetchDelayWhenSyncedWithRemote
    else
      initialDelayWhenHaveNotSyncedWithRemote
}