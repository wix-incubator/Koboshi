package com.wix.hoopoe.koboshi.marshaller

import scala.reflect.ClassTag

trait Marshallers {
  def marshaller[T : ClassTag]: Marshaller[T]

}
