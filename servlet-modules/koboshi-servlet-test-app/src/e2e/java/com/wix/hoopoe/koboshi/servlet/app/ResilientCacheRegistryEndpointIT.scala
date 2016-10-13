package com.wix.hoopoe.koboshi.servlet.app

import java.net.{HttpURLConnection, URL}

import org.specs2.mutable.SpecificationWithJUnit

class ResilientCacheRegistryEndpointIT extends SpecificationWithJUnit {

  "ResilientCacheRegistryEndpoint" should {
    "ignore requests to non existing caches" in new EmbeddedEnvironmentContext {
      def callApp(url: String) = call(trimTrailingSlash(env.appDriver.address) + url)

      callApp("/koboshi/non_existent/fetch") ==== 200
    }
  }

  def trimTrailingSlash(address: URL): String = address.toExternalForm.dropRight(1)

  def call(url: String): Int = {
    val connection = new URL(url).openConnection().asInstanceOf[HttpURLConnection]
    connection.connect()
    val responseCode = connection.getResponseCode
    connection.disconnect()
    responseCode
  }

}
