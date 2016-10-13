package com.wix.hoopoe.koboshi.servlet

import javax.servlet.http.{HttpServlet, HttpServletRequest, HttpServletResponse}

import com.wix.hoopoe.koboshi.registry.RemoteDataFetcherRegistry
import com.wix.hoopoe.koboshi.servlet.ResilientCacheRegistryEndpoint.RegistryKey

class ResilientCacheRegistryEndpoint extends HttpServlet {

  override def doGet(req: HttpServletRequest, resp: HttpServletResponse): Unit = {
    val namespace = extractNamespace(req.getRequestURI)
    registry.lookup(namespace).foreach(r => r.fetchNow())
  }

  def registry: RemoteDataFetcherRegistry =
    getServletContext.getAttribute(RegistryKey).asInstanceOf[RemoteDataFetcherRegistry]

  def extractNamespace(requestURI: String): String = {
    val requestParts = requestURI.split("/")
    val partsWithoutFetchConstant = requestParts.init
    val namespace = partsWithoutFetchConstant.last
    namespace
  }
}

object ResilientCacheRegistryEndpoint {
  val RegistryKey = "koboshi.registry"
}
