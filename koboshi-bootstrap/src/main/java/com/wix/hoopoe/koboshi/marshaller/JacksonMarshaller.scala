package com.wix.hoopoe.koboshi.marshaller

import java.io.{ByteArrayInputStream, IOException}

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.{JavaType, ObjectMapper}
import com.wix.hoopoe.koboshi.cache.TimestampedData

import scala.reflect._

class JacksonMarshaller[T : ClassTag](mapper: ObjectMapper) extends Marshaller[T] {
  mapper.addMixInAnnotations(classOf[TimestampedData[T]], classOf[TimestampedDataIgnoreUnknownMixin])

  @throws(classOf[IOException])
  override def unmarshall(timestampedData: Array[Byte]): TimestampedData[T] =
    mapper.readValue(new ByteArrayInputStream(timestampedData), getValueType)

  private def getValueType: JavaType =
    mapper.getTypeFactory.constructParametricType(classOf[TimestampedData[T]], classTag[T].runtimeClass)

  @throws(classOf[IOException])
  override def marshall(timestampedData: TimestampedData[T]): Array[Byte] = mapper.writeValueAsBytes(timestampedData)
}

@JsonIgnoreProperties(ignoreUnknown = true)
private[marshaller] class TimestampedDataIgnoreUnknownMixin