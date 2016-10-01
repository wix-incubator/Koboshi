package com.wix.hoopoe.koboshi.remote;

/**
 * @author ittaiz
 * @since 7/16/13
 */

import com.wix.hoopoe.koboshi.cache.TimestampedData;
import com.wix.hoopoe.koboshi.cache.persistence.PersistentCache;
import com.wix.hoopoe.koboshi.cache.transience.TransientCache;
import com.wix.hoopoe.koboshi.example.SomeDataType;
import com.wix.hoopoe.koboshi.report.FetchingException;
import com.wix.hoopoe.koboshi.report.RemoteDataFetchingReporter;
import com.wix.hoopoe.koboshi.scheduler.SystemClock;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.jmock.Expectations;
import org.jmock.States;
import org.jmock.auto.Mock;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.joda.time.Instant;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
public class BlockingRemoteDataFetcherTest {
    //TODO see if i can kill this test in favor of an acceptance test/ scheduled remote data fetcher IT
    private static final SomeDataType INITIAL_LOCAL_DATA = new SomeDataType("CACHED DATA");
    private static final SomeDataType INITIAL_REMOTE_DATA = new SomeDataType("REMOTE DATA");
    public static final SomeDataType SOME_DATA_TYPE = new SomeDataType();
    @Rule
    public JUnitRuleMockery context = new JUnitRuleMockery();

    @Mock
    private RemoteDataSource<SomeDataType> remoteDataSource;

    @Mock
    private PersistentCache<SomeDataType> persistentCache;

    @Mock
    private TransientCache<SomeDataType> transientCache;

    @Mock
    private RemoteDataFetchingReporter reporter;

    private BlockingRemoteDataFetcher<SomeDataType> blockingRemoteDataFetcher;

    @Before
    public void setup() {
        blockingRemoteDataFetcher = new BlockingRemoteDataFetcher<>(remoteDataSource, persistentCache,
                transientCache, reporter, new SystemClock());
    }

    @Test(expected = FetchingException.class)
    public void shouldErrorWhenFetchingFromRemoteFails() throws IOException {
        setupFailureToFetchFromRemote();
        context.checking(new Expectations() {{
            ignoring(transientCache);
        }});
        blockingRemoteDataFetcher.fetchNow();
    }

    @Test(expected = FetchingException.class)
    public void shouldErrorWhenFailingToWriteToLocalCaches() throws IOException {
        setupRemoteToReturn(SOME_DATA_TYPE);
        context.checking(new Expectations() {{
            allowing(transientCache).write(with(dataIgnoringTimestamp(SOME_DATA_TYPE)));
            will(throwException(new RuntimeException()));
            ignoring(transientCache);
        }});
        blockingRemoteDataFetcher.fetchNow();
    }

    @Test
    public void shouldRetrieveDataFromPersistentCacheOnInit() throws IOException {
        setupRemoteToReturn(INITIAL_REMOTE_DATA);
        setupPersistentCacheWith(INITIAL_LOCAL_DATA);
        context.checking(new Expectations() {{
            oneOf(transientCache).write(with(dataIgnoringTimestamp(INITIAL_LOCAL_DATA)));
        }});
        blockingRemoteDataFetcher.init();
    }

    @Test
    public void shouldUpdateTheLocalCachesOnInitIfNoDataExistsInPersistentCache() throws IOException {
        setupEmptyPersistentCache();
        setupRemoteToReturn(INITIAL_REMOTE_DATA);
        context.checking(new Expectations() {{
            final States state = context.states("update from remote state machine").startsAs("no update");
            oneOf(transientCache).write(with(dataIgnoringTimestamp(INITIAL_REMOTE_DATA)));
            then(state.is("transient cache data updated"));
            oneOf(persistentCache).write(with(dataIgnoringTimestamp(INITIAL_REMOTE_DATA)));
            when(state.is("transient cache data updated"));
            ignoring(transientCache);
        }});
        blockingRemoteDataFetcher.init();
    }

    @Test(expected = IllegalStateException.class)
    public void shouldInitOnlyOnce() throws IOException {
        setupEmptyPersistentCache();
        context.checking(new Expectations() {{
            oneOf(remoteDataSource).fetch();

            ignoring(persistentCache);
            ignoring(transientCache);
        }});

        blockingRemoteDataFetcher.init();
        blockingRemoteDataFetcher.init();
    }

    @Test
    public void givenAnErrorReadingPersistentCacheOnInitThenRetrievesDataFromRemote() throws IOException {
        ignoreReporter();
        context.checking(new Expectations() {{
            allowing(persistentCache).readTimestamped();
            will(throwException(new RuntimeException("Should have been caught")));
            oneOf(remoteDataSource).fetch();

            ignoring(persistentCache);
            ignoring(transientCache);
        }});

        blockingRemoteDataFetcher.init();
    }

    @Test
    public void givenAnErrorReadingPersistentCacheOnInitThenTheErrorIsReported() throws IOException {
        context.checking(new Expectations() {{
            allowing(persistentCache).readTimestamped();
            final RuntimeException runtimeException = new RuntimeException("Should have been caught");
            will(throwException(runtimeException));
            oneOf(reporter).cannotReadFromPersistentCache(runtimeException);

            ignoring(remoteDataSource);
            ignoring(persistentCache);
            ignoring(transientCache);
        }});

        blockingRemoteDataFetcher.init();
    }

    @Test
    public void givenAnErrorWritingToPersistentCacheThenTheErrorIsReported() throws IOException {
        setupRemoteToReturn(SOME_DATA_TYPE);
        context.checking(new Expectations() {{
            ignoring(transientCache);
            allowing(persistentCache).write(with(any(TimestampedData.class)));
            final RuntimeException runtimeException = new RuntimeException("Should have been caught");
            will(throwException(runtimeException));
            oneOf(reporter).cannotWriteToPersistentCache(runtimeException);
        }});

        blockingRemoteDataFetcher.fetchNow();
    }

    @Test
    public void givenAnErrorInRemoteFetchingWhenInitializingThenTheErrorIsReported() throws IOException {
        setupEmptyPersistentCache();
        setupFailureToFetchFromRemote();
        ignoreTransientCache();
        context.checking(new Expectations() {{
            oneOf(reporter).cannotCompleteInitializingFromRemote(with(any(FetchingException.class)));
        }});

        blockingRemoteDataFetcher.init();
    }

    @Test
    public void hasSyncedWithRemoteReturnsTrueIfSuccessfullyRetrievedDataFromRemoteOnInit() throws IOException {
        setupEmptyPersistentCache();
        setupRemoteToReturn(SOME_DATA_TYPE);
        ignoreReporter();

        context.checking(new Expectations() {{
            ignoring(transientCache);
            ignoring(persistentCache);
        }});

        blockingRemoteDataFetcher.init();
        assertThat(blockingRemoteDataFetcher.hasSyncedWithRemote(), is(true));
    }

    @Test
    public void hasSyncedWithRemoteReturnsFalseIfFailingToRetrievedDataFromRemoteOnInit() throws IOException {
        setupEmptyPersistentCache();
        setupFailureToFetchFromRemote();
        ignoreTransientCache();
        ignoreReporter();

        blockingRemoteDataFetcher.init();
        assertThat(blockingRemoteDataFetcher.hasSyncedWithRemote(), is(false));
    }

    @Test
    public void hasSyncedWithRemoteReturnsTrueIfSuccessfullyRetrievedDataFromRemoteDuringSyncFetching() throws IOException {
        setupRemoteToReturn(SOME_DATA_TYPE);
        ignoreReporter();

        context.checking(new Expectations() {{
            ignoring(transientCache);
            ignoring(persistentCache);
        }});

        blockingRemoteDataFetcher.fetchNow();
        assertThat(blockingRemoteDataFetcher.hasSyncedWithRemote(), is(true));
    }

    private void ignoreTransientCache() {
        context.checking(new Expectations() {{
            ignoring(transientCache);
        }});
    }

    private void ignoreReporter() {
        context.checking(new Expectations() {{
            ignoring(reporter);
        }});
    }

    private void setupEmptyPersistentCache() {
        setupPersistentCacheWithTimestampedData(null);
    }

    private void setupPersistentCacheWith(final SomeDataType data) {
        setupPersistentCacheWithTimestampedData(new TimestampedData<>(data, new Instant()));
    }

    private void setupPersistentCacheWithTimestampedData(final TimestampedData<SomeDataType> timestampedData) {
        context.checking(new Expectations() {{
            allowing(persistentCache).readTimestamped();
            will(returnValue(timestampedData));
        }});
    }

    private void setupFailureToFetchFromRemote() throws IOException {
        context.checking(new Expectations() {{
            allowing(remoteDataSource).fetch();
            will(throwException(new IOException()));
        }});
    }

    private void setupRemoteToReturn(final SomeDataType data) throws IOException {
        context.checking(new Expectations() {{
            allowing(remoteDataSource).fetch();
            will(returnValue(data));
        }});
    }

    private Matcher<TimestampedData<SomeDataType>> dataIgnoringTimestamp(final SomeDataType data) {
        return new TypeSafeMatcher<TimestampedData<SomeDataType>>() {
            private final Matcher<SomeDataType> inputDataMatcher = equalTo(data);
            @Override
            protected boolean matchesSafely(final TimestampedData<SomeDataType> someDataTypeTimestampedData) {
                return inputDataMatcher.matches(someDataTypeTimestampedData.data());
            }

            @Override
            public void describeTo(final Description description) {
                inputDataMatcher.describeTo(description);
            }
        };
    }

}
