package com.wix.hoopoe.koboshi.cache.persistence

import java.io.IOException
import java.nio.file.{Files, Path}

import com.wix.hoopoe.koboshi.cache.TimestampedData
import com.wix.hoopoe.koboshi.marshaller.Marshaller
import com.wix.hoopoe.koboshi.report.PersistenceException
import org.slf4j.{Logger, LoggerFactory}

object DiskCache {
  private val logger: Logger = LoggerFactory.getLogger("diskCache")
}

class DiskCache[T](diskCache: Path, marshaller: Marshaller[T]) extends PersistentCache[T] {

  override def readTimestamped(): TimestampedData[T] = {
    try {
      if (!Files.isReadable(diskCache)) {
        return null.asInstanceOf[TimestampedData[T]]
      }
      val marshalledLocalData = Files.readAllBytes(diskCache)
      report("read", marshalledLocalData)
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
      report("writing", marshalledLocalData)
      Files.write(diskCache, marshalledLocalData)
    }
    catch {
      case ioe: IOException =>
        throw new PersistenceException(String.format("Failed persisting disk cache, file=%s", diskCache), ioe)
    }
  }

  private def report(action: String, marshalledLocalData: Array[Byte]): Unit = {
    if (DiskCache.logger.isTraceEnabled) {
      DiskCache.logger.trace(action + " " + marshalledLocalData.length + " number of chars using " + diskCache)
    }
  }
}