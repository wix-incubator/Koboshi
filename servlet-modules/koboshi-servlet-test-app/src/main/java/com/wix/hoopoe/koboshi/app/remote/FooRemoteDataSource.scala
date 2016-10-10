package com.wix.hoopoe.koboshi.app.remote

import java.net.URL

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.wix.hoopoe.koboshi.app.domain.Foo
import com.wix.hoopoe.koboshi.remote.RemoteDataSource

class FooRemoteDataSource(remotePort: Int) extends RemoteDataSource[Foo] {
  private val RemoteUrl = s"http://127.0.0.1:$remotePort"
  private val objectMapper = new ObjectMapper().registerModule(DefaultScalaModule)
  override def fetch(): Foo = {
    //The logic here is based on your remote
    //Can be calling to DB, RPC, plain rest with your favorite http client or whatever you can think about
    val remoteUrl = new URL(RemoteUrl)
    val cnx = remoteUrl.openConnection()
    val foo = objectMapper.readValue(cnx.getInputStream,classOf[Foo])
    foo
  }
}
