package com.wix.hoopoe.koboshi.it

import java.net.URL

import com.wix.hoopoe.http.testkit.EmbeddedHttpProbe
import org.specs2.matcher.Matcher
import org.specs2.mutable.{BeforeAfter, SpecificationWithJUnit}
import org.specs2.specification.Scope
import spray.http.HttpRequest

class RemoteDataFetcherDriverTest extends SpecificationWithJUnit {

  trait Context extends Scope with BeforeAfter {

    val probe = new EmbeddedHttpProbe()

    def httpRequestWhosePath(path: Matcher[String]): Matcher[HttpRequest] = path ^^ {
      (_: HttpRequest).uri.path.toString()
    }

    def before {
      probe.doStart()
    }

    def after {
      probe.doStop()
    }
  }

  "RemoteDataFetcherDriver should allow connecting to a server " >> {
    "which has koboshi on a custom path" in new Context {
      RemoteDataFetcherDriver("localhost", probe.actualPort, "some_custom_path").fetch[String]()

      probe.requests must contain(httpRequestWhosePath(startWith("/some_custom_path/koboshi")))
    }

    "whose address ends with a slash" in new Context {
      RemoteDataFetcherDriver(new URL(s"http://localhost:${probe.actualPort}/")).fetch[String]()

      probe.requests must contain(httpRequestWhosePath(startWith("/koboshi")))
    }

    "which has koboshi on a custom path with a leading slash" in new Context {
      RemoteDataFetcherDriver("localhost", probe.actualPort, "/leading_slash").fetch[String]()

      probe.requests must contain(httpRequestWhosePath(startWith("/leading_slash")))
    }
  }
}
