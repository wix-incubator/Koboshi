package com.wix.hoopoe.koboshi.app.endpoint

import java.nio.file.Files
import javax.servlet.http.{HttpServlet, HttpServletRequest, HttpServletResponse}

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.wix.hoopoe.koboshi.app.domain.Foo
import com.wix.hoopoe.koboshi.app.remote.FooRemoteDataSource
import com.wix.hoopoe.koboshi.cache.ReadOnlyLocalCache
import com.wix.hoopoe.koboshi.cache.defaults.ResilientCaches

class FooEndpoint extends HttpServlet {
  private val objectMapper = new ObjectMapper().registerModule(DefaultScalaModule)
  private val FooCacheKey = "koboshi.cache.foo"

  //A bit of a hack- in production you should probably do this composition in
  // Main, your favorite DI, ServletContainerInitializer or just like this if you have no alternatives
  override def init(): Unit = {
    val persistentCopyFile = Files.createTempFile("koboshi", "foo").toFile
    val remotePort = getServletContext.getAttribute("remote.port").asInstanceOf[Int]
    //In prod don't use a temp file
    val cache = ResilientCaches.resilientCaches(persistentCopyFile)
        .aResilientInitializedCache[Foo](new FooRemoteDataSource(remotePort))
    getServletContext.setAttribute(FooCacheKey, cache)
  }

  override def doGet(req: HttpServletRequest, resp: HttpServletResponse): Unit = {
    val cache = req.getServletContext.getAttribute(FooCacheKey).asInstanceOf[ReadOnlyLocalCache[Foo]]
    val foo = cache.read()
    objectMapper.writeValue(resp.getOutputStream, foo)
  }
}

