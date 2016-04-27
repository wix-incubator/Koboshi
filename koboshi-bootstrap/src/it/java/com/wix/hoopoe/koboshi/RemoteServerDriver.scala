package com.wix.hoopoe.koboshi

import java.util.concurrent.atomic.AtomicReference

import com.fasterxml.jackson.databind.ObjectMapper
import com.wix.hoopoe.http.testkit.EmbeddedHttpProbe
import com.wix.hoopoe.koboshi.example.SomeDataType
import spray.http.ContentTypes._
import spray.http.{HttpEntity, HttpResponse, StatusCodes}

class RemoteServerDriver {
  private val fakeRemoteServer = new EmbeddedHttpProbe
  private final val data = new AtomicReference[SomeDataType]
  private val mapper = new ObjectMapper()
  startRespondingStoredData()

  def start(): Unit = fakeRemoteServer.doStart()

  def stop(): Unit = fakeRemoteServer.doStop()

  def respondWith(remoteData: SomeDataType): Unit = {
    data.set(remoteData)
    startRespondingStoredData()
  }
  def port: Int = fakeRemoteServer.actualPort

  def stopResponding(): Unit = {
    serverShouldRespondWith(HttpResponse(status = StatusCodes.BadGateway))
  }

  private def startRespondingStoredData(): Unit =
    serverShouldRespondWith(HttpResponse(entity = HttpEntity(`application/json`, mapper.writeValueAsString(data.get()))))

  private def serverShouldRespondWith(response : HttpResponse): Unit = {
    fakeRemoteServer.reset()
    fakeRemoteServer.handlers += {
      case _ =>
        response
    }
  }
}