package com.wix.hoopoe.koboshi.cache.transience

import java.util.concurrent.atomic.AtomicReference

import com.wix.hoopoe.koboshi.cache.TimestampedData

class AtomicReferenceCache[T]() extends TransientCache[T] {
    private final val timestampedData: AtomicReference[TimestampedData[T]] = new AtomicReference[TimestampedData[T]]

    override def read(): T = readTimestamped.data

    override def readTimestamped(): TimestampedData[T] = timestampedData.get

    override def write(data: TimestampedData[T]): Unit = timestampedData.set(data)
}