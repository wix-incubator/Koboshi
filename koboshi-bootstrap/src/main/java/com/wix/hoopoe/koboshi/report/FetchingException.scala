package com.wix.hoopoe.koboshi.report

case class FetchingException(message: String, cause: Throwable) extends RuntimeException(message, cause)
