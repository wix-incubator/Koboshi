package com.wix.hoopoe.koboshi.servlet.app

class EmbeddedEnvironment {
  val appDriver: AppDriver = new AppDriver(this)
  val remoteDriver: RemoteDriver = new RemoteDriver

  def start(): Unit = {
    remoteDriver.start()
    appDriver.start()
  }

  def stop(): Unit = {
    appDriver.stop()
    remoteDriver.stop()
  }
}
