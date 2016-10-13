package com.wix.hoopoe.koboshi.servlet.app

import org.specs2.mutable.BeforeAfter
import org.specs2.specification.Scope

trait EmbeddedEnvironmentContext extends Scope with BeforeAfter {
  val env = new EmbeddedEnvironment

  override def before = env.start()

  override def after = env.stop()
}
