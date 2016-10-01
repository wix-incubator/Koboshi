package com.wix.hoopoe.koboshi.it

import java.net.{HttpURLConnection, URL}

import com.wix.hoopoe.koboshi.namespace.NamespaceCreator.cacheNamespace

import scala.reflect.ClassTag

class RemoteDataFetcherDriver(address: URL) {
  private val addressExternalForm = address.toExternalForm.stripSuffix("/")

  @deprecated("use apply methods or single arg ctor", "10/10/16")
  def this(host: String, port: Int, urlSuffix: String) =
    this(new URL("http", host, port, "/" + urlSuffix))

  @deprecated("use apply methods or single arg ctor", "10/10/16")
  def this(host: String, port: Int) = this(host, port, "")

  def fetch(customNamespace: String): Unit = {
    val url = koboshiFetchUrlFor(customNamespace)
    val connection = url.openConnection().asInstanceOf[HttpURLConnection]
    connection.setRequestMethod("GET")
    connection.connect()
    connection.getResponseCode
    connection.disconnect()
  }

  private def koboshiFetchUrlFor(customNamespace: String): URL = {
    new URL(s"$addressExternalForm/koboshi/$customNamespace/fetch")
  }

  def fetch[T: ClassTag](): Unit = fetch(cacheNamespace)

  def fetch(dataClass: Class[_]): Unit =
    fetch()(ClassTag(dataClass))
}

object RemoteDataFetcherDriver {
  def apply(host: String, port: Int): RemoteDataFetcherDriver = apply(host, port, "")

  def apply(host: String, port: Int, path: String): RemoteDataFetcherDriver =

    apply(new URL("http", host, port, "/" + path.stripPrefix("/")))

  def apply(address: URL): RemoteDataFetcherDriver = new RemoteDataFetcherDriver(address)
}