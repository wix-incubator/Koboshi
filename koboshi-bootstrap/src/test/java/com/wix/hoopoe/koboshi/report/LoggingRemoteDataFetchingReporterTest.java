package com.wix.hoopoe.koboshi.report;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.wix.hoopoe.koboshi.RemoteDataFetchingLogDriver;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URI;

import static com.wix.hoopoe.koboshi.RemoteDataFetchingLogDriver.aRemoteDataFetchingLogDriver;
import static org.hamcrest.CoreMatchers.is;

/**
 * @author ittaiz
 * @since 7/17/13
 */
public class LoggingRemoteDataFetchingReporterTest {

    private static final String DATA_TYPE = String.class.getName();
    private static final byte[] CONTENT = "CONTENT".getBytes();
    private static final URI CACHE_URI = new File("IRRELEVANT").toURI();
    private RemoteDataFetchingLogDriver remoteDataFetchingLogDriver;
    private LoggingRemoteDataFetchingReporter loggingRemoteDataFetchingReporter;

    @Before
    public void setup() {
        Logger logger = (Logger) LoggerFactory.getLogger("foo");
        remoteDataFetchingLogDriver = aRemoteDataFetchingLogDriver(logger);
        loggingRemoteDataFetchingReporter = new LoggingRemoteDataFetchingReporter(logger, DATA_TYPE);
    }

    @Test
    public void logsSyncRemoteFailureMessage() {
        final IOException exception = new IOException("IRRELEVANT");

        loggingRemoteDataFetchingReporter.cannotCompleteFetchingFromRemote(exception);

        remoteDataFetchingLogDriver.reportsWithException(Level.ERROR, is("failure syncing with remote for " + DATA_TYPE));
    }

    @Test
    public void logsSyncRemoteAttemptMessage() {

        loggingRemoteDataFetchingReporter.attemptingToFetchFromRemote();

        remoteDataFetchingLogDriver.reports(Level.TRACE, is("attempting to sync with remote for " +
                DATA_TYPE));
    }

    @Test
    public void logsPersistentCacheReadFailureMessage() {
        final RuntimeException exception = new RuntimeException("IRRELEVANT");

        loggingRemoteDataFetchingReporter.cannotReadFromPersistentCache(exception);

        remoteDataFetchingLogDriver.reportsWithException(Level.WARN, is("failure reading persistent cache for " + DATA_TYPE));
    }

    @Test
    public void logsPersistentCacheWriteFailureMessage() {
        final RuntimeException exception = new RuntimeException("IRRELEVANT");

        loggingRemoteDataFetchingReporter.cannotWriteToPersistentCache(exception);

        remoteDataFetchingLogDriver.reportsWithException(Level.WARN, is("failure writing to persistent cache for " + DATA_TYPE));
    }

    @Test
    public void logsInitRemoteFailureMessage() {
        final RuntimeException exception = new RuntimeException("IRRELEVANT");

        loggingRemoteDataFetchingReporter.cannotCompleteInitializingFromRemote(exception);

        remoteDataFetchingLogDriver.reportsWithException(Level.ERROR, is("failure initializing from remote for " + DATA_TYPE));
    }

    @Test
    public void logsShutdownMessage() {
        loggingRemoteDataFetchingReporter.initiatingShutdown();

        remoteDataFetchingLogDriver.reports(Level.INFO, is("initiating shutdown for " + DATA_TYPE));
    }

    @Test
    public void logsPersistentCacheReadAttemptMessage() {
        loggingRemoteDataFetchingReporter.readFromPersistentCache(CACHE_URI, CONTENT);

        remoteDataFetchingLogDriver.reports(Level.TRACE, is("read "+CONTENT.length+" bytes using " + CACHE_URI));
    }

    @Test
    public void logsPersistentCacheWriteAttemptMessage() {
        loggingRemoteDataFetchingReporter.writeToPersistentCache(CACHE_URI, CONTENT);

        remoteDataFetchingLogDriver.reports(Level.TRACE, is("writing "+CONTENT.length+" bytes using " + CACHE_URI));
    }

}
