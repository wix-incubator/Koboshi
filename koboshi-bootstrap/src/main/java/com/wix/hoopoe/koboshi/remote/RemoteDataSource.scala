package com.wix.hoopoe.koboshi.remote

import java.io.IOException

trait RemoteDataSource[T] {
  @throws(classOf[IOException])
  def fetch(): T
}