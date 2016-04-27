package com.wix.hoopoe.koboshi.cache.transience

import com.wix.hoopoe.koboshi.cache.{ReadOnlyLocalCache, TimestampedLocalCache}

/**
 * Tagging interface
 */
trait TransientCache[T] extends ReadOnlyLocalCache[T] with TimestampedLocalCache[T]