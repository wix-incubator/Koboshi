package com.wix.hoopoe.koboshi.cache.persistence

import java.io.IOException
import java.nio.file.{Files, Path}

import com.wix.hoopoe.koboshi.cache.TimestampedData
import com.wix.hoopoe.koboshi.marshaller.Marshaller
import com.wix.hoopoe.koboshi.report.{PersistenceException, RemoteDataFetchingReporter}

class DiskCache[T](diskCache: Path, marshaller: Marshaller[T], reporter: RemoteDataFetchingReporter) extends PersistentCache[T] {

  override def readTimestamped(): TimestampedData[T] = {
    try {
      if (!Files.isReadable(diskCache)) {
        return null.asInstanceOf[TimestampedData[T]]
      }
      val marshalledLocalData = Files.readAllBytes(diskCache)
      reporter.readFromPersistentCache(diskCache.toUri, marshalledLocalData)
      marshaller.unmarshall(marshalledLocalData)
    }
    catch {
      case ioe: IOException =>
        throw new PersistenceException(s"Failed reading disk cache, file=$diskCache", ioe)
    }
  }

  override def write(timestampedData: TimestampedData[T]): Unit = {
    try {
      val marshalledLocalData = marshaller.marshall(timestampedData)
      reporter.writeToPersistentCache(diskCache.toUri, marshalledLocalData)
      Files.write(diskCache, marshalledLocalData)
    }
    catch {
      case ioe: IOException =>
        throw new PersistenceException(String.format("Failed persisting disk cache, file=%s", diskCache), ioe)
    }
  }

}