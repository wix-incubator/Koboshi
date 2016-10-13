package com.wix.hoopoe.koboshi.servlet.app

import java.net.URL

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.wix.hoopoe.koboshi.app.domain.Foo
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.webapp.WebAppContext

class AppDriver(env: EmbeddedEnvironment) {
  private val objectMapper = new ObjectMapper().registerModule(DefaultScalaModule)
  private val (server, context) = createServerAndContext()

  def getFoo(): Foo = {
    val fooUrl = server.getURI.resolve("/foo").toURL
    val cnx = fooUrl.openConnection()
    val foo = objectMapper.readValue(cnx.getInputStream, classOf[Foo])
    foo
  }

  def address: URL = server.getURI.toURL

  def start(): Unit = {
    context.setAttribute("remote.port", env.remoteDriver.port)
    server.start()
  }

  def stop(): Unit = {
    server.stop()
  }

  private def createServerAndContext(): (Server, WebAppContext) = {
    val server = new Server(0)
    val context = new WebAppContext() //using a WebAppContext to use web.xml
    context.setDescriptor("src/main/webapp/WEB-INF/web.xml")
    context.setResourceBase(".") //have to set something or else the server starts unavailable
    server.setHandler(context)
    (server, context)
  }
}