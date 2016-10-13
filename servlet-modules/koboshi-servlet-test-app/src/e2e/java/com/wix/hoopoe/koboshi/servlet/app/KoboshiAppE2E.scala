package com.wix.hoopoe.koboshi.servlet.app

import com.wix.hoopoe.koboshi.app.domain.Foo
import com.wix.hoopoe.koboshi.it.RemoteDataFetcherDriver
import org.specs2.mutable.SpecificationWithJUnit


class KoboshiAppE2E extends SpecificationWithJUnit {

  "koboshi in a servlet based web application" should {
    "start a single element resilient cache with data from remote" in new EmbeddedEnvironmentContext {
      env.appDriver.getFoo() must not(beNull[Foo])
    }

    "support on demand fetching via http" in new EmbeddedEnvironmentContext {
      val freshFoo = new Foo("fresh remote data")
      val remoteDataFetcherDriver = RemoteDataFetcherDriver(env.appDriver.address)

      env.remoteDriver.respondWith(freshFoo)
      remoteDataFetcherDriver.fetch[Foo]()

      env.appDriver.getFoo() ==== freshFoo
    }
  }

}



