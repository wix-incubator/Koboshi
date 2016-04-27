package com.wix.hoopoe.koboshi.cache.persistence

import com.wix.hoopoe.koboshi.cache.TimestampedLocalCache

/**
 * Tagging interface
 */
trait PersistentCache[T] extends TimestampedLocalCache[T]