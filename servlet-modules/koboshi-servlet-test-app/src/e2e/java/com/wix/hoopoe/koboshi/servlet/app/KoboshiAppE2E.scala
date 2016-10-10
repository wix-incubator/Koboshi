package com.wix.hoopoe.koboshi.servlet.app

import com.wix.hoopoe.koboshi.app.domain.Foo
import org.specs2.mutable.SpecificationWithJUnit


class KoboshiAppE2E extends SpecificationWithJUnit {

  "koboshi in a servlet based web application" should {
    "start a single element resilient cache with data from remote" in {
      val env = new EmbeddedEnvironment()
      env.start()
      try {
        env.appDriver.getFoo() must not(beNull[Foo])
      } finally {
        env.stop()
      }
    }
  }
}


