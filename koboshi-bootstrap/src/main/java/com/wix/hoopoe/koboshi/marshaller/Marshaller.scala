package com.wix.hoopoe.koboshi.marshaller

import java.io.IOException

import com.wix.hoopoe.koboshi.cache.TimestampedData

trait Marshaller[T] {

  @throws(classOf[IOException])
  def unmarshall(timestampedData: Array[Byte]): TimestampedData[T]

  @throws(classOf[IOException])
  def marshall(timestampedData: TimestampedData[T]): Array[Byte]
}