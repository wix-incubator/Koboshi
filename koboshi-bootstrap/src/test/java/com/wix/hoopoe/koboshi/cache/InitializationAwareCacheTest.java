package com.wix.hoopoe.koboshi.cache;

import com.wix.hoopoe.koboshi.cache.transience.AtomicReferenceCache;
import org.joda.time.Instant;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static junit.framework.TestCase.fail;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author ittaiz
 * @since 12/23/13
 */
public class InitializationAwareCacheTest {

    private static final String SOME_DATA = "some data";
    private InitializationAwareCache<String> initializationAwareCache =
            new InitializationAwareCache<>(new AtomicReferenceCache<String>());

    @Test(expected = IllegalStateException.class)
    public void throwsAnErrorWhenReadingBeforeFirstWrite() {
        initializationAwareCache.read();
    }

    @Test
    public void returnsValueFromReadAfterFirstWrite() {
        initializationAwareCache.write(timestampTheData(SOME_DATA));
        assertThat(initializationAwareCache.read(), is(SOME_DATA));
    }

    private TimestampedData<String> timestampTheData(final String data) {
        return new TimestampedData<>(data, new Instant());
    }

    @Test(timeout = 1000L)
    public void throwsAnErrorWhenReadingDuringFirstWrite() throws InterruptedException {
        BlockingCacheDriver blockingCacheDriver = new BlockingCacheDriver();
        InitializationAwareCacheDriver initializationAwareCacheDriver = new InitializationAwareCacheDriver(blockingCacheDriver);

        blockingCacheDriver.lockWrite();
        initializationAwareCacheDriver.writeAsynchronously(timestampTheData(SOME_DATA));
        try {
            initializationAwareCacheDriver.read();
            fail("Read passed but should have been disabled (via exception throwing)");
        } catch (IllegalStateException e) {
            //we want an exception to happen
        } finally {
            blockingCacheDriver.unlockWrite();
        }
    }

    @Test(timeout = 1000L)
    public void throwsAnErrorWhenReadingWithTimestampDuringFirstWrite() throws InterruptedException {
        BlockingCacheDriver blockingCacheDriver = new BlockingCacheDriver();
        InitializationAwareCacheDriver initializationAwareCacheDriver = new InitializationAwareCacheDriver(blockingCacheDriver);

        blockingCacheDriver.lockWrite();
        initializationAwareCacheDriver.writeAsynchronously(timestampTheData(SOME_DATA));
        try {
            initializationAwareCacheDriver.readTimestamped();
            fail("Read passed but should have been disabled (via exception throwing)");
        } catch (IllegalStateException e) {
            //we want an exception to happen
        } finally {
            blockingCacheDriver.unlockWrite();
        }
    }

    private static class BlockingCacheDriver extends AtomicReferenceCache<String> {
        private Lock writeLock = new ReentrantLock();
        private CountDownLatch startedWrite = new CountDownLatch(1);

        @Override
        public void write(final TimestampedData<String> data) {
            startedWrite.countDown();
            writeLock.lock();
            super.write(data);
        }

        public void lockWrite() {
            writeLock.lock();
        }
        public void unlockWrite(){
            writeLock.unlock();
        }

        public void waitToStartWrite() {
            try {
                startedWrite.await();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static class InitializationAwareCacheDriver {
        private final InitializationAwareCache<String> initializationAwareCache;
        private final BlockingCacheDriver blockingCacheDriver;

        public InitializationAwareCacheDriver(BlockingCacheDriver underlyingCache) {
            initializationAwareCache = new InitializationAwareCache<>(underlyingCache);
            blockingCacheDriver = underlyingCache;
        }

        public void writeAsynchronously(final TimestampedData<String> data) {
            Executors.newSingleThreadExecutor().submit(new Runnable() {
                @Override
                public void run() {
                    initializationAwareCache.write(data);
                }
            });
            blockingCacheDriver.waitToStartWrite();
        }

        public void read() {
            initializationAwareCache.read();
        }

        public void readTimestamped() {
             initializationAwareCache.readTimestamped();
        }
    }

}
