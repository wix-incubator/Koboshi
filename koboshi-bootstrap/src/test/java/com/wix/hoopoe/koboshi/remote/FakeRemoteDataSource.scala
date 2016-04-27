package com.wix.hoopoe.koboshi.remote

/**
 * @author ittaiz
 * @since 8/3/14
 */
class FakeRemoteDataSource[T] extends RemoteDataSource[T] {
  def fetch: T = f()

  def act(action: () => T): Unit = this.f = action

  private var f: () => T = {
    null
  }
}