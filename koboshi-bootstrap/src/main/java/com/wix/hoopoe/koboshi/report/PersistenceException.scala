package com.wix.hoopoe.koboshi.report

case class PersistenceException(message: String, cause: Throwable) extends RuntimeException(message, cause)
