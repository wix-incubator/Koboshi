package com.wix.hoopoe.koboshi.servlet.app

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.wix.hoopoe.http.testkit.EmbeddedHttpProbe
import com.wix.hoopoe.koboshi.app.domain.Foo
import spray.http.{ContentTypes, HttpEntity, HttpRequest, HttpResponse}

class RemoteDriver {
  def port: Int = fakeRemote.actualPort

  def stop(): Unit = fakeRemote.doStop()

  def start(): Unit = fakeRemote.doStart()

  private val fakeRemote: EmbeddedHttpProbe = createFakeRemote()
  private val objectMapper = new ObjectMapper().registerModule(DefaultScalaModule)

  private def createFakeRemote() = {
    val probe = new EmbeddedHttpProbe
    probe.handlers += {
      case HttpRequest(_, _, _, _, _) =>
        HttpResponse(entity = HttpEntity(ContentTypes.`application/json`, objectMapper.writeValueAsBytes(new Foo("initial"))))
    }
    probe
  }
}
