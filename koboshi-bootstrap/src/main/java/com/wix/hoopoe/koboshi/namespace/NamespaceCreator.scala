package com.wix.hoopoe.koboshi.namespace

import scala.reflect._

object NamespaceCreator {
  def cacheNamespace[T : ClassTag]: String = classTag[T].runtimeClass.getName
}