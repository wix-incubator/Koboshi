package com.wix.hoopoe.koboshi.marshaller

import com.fasterxml.jackson.databind.ObjectMapper

import scala.reflect.ClassTag

class JacksonMarshallers(objectMapper: ObjectMapper) extends Marshallers {
  override def marshaller[T: ClassTag]: Marshaller[T] = new JacksonMarshaller[T](objectMapper)
}
