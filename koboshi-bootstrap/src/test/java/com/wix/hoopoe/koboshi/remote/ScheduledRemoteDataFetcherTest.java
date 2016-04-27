package com.wix.hoopoe.koboshi.remote;


import com.wix.hoopoe.koboshi.example.SomeDataType;
import com.wix.hoopoe.koboshi.report.RemoteDataFetchingReporter;
import com.wix.hoopoe.koboshi.scheduler.DeterministicSchedulers;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.jmock.Expectations;
import org.jmock.States;
import org.jmock.auto.Mock;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.jmock.lib.concurrent.DeterministicScheduler;
import org.jmock.lib.concurrent.Synchroniser;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author: ittaiz
 * @since: 6/25/13
 */
public class ScheduledRemoteDataFetcherTest {
    @Rule
    public JUnitRuleMockery context = new JUnitRuleMockery() {{
        setThreadingPolicy(new Synchroniser());
    }};

    @Mock
    private RemoteDataFetcher<SomeDataType> remoteDataFetcher;

    @Mock
    private RemoteDataFetchingReporter remoteDataFetchingReporter;

    private DeterministicScheduler scheduler;
    private ScheduledRemoteDataFetcher<SomeDataType> scheduledRemoteDataFetcher;

    @Before
    public void setup() {
        scheduler = new DeterministicSchedulers().aScheduler("IRRELEVANT NAMESPACE");
        scheduledRemoteDataFetcher = new
                ScheduledRemoteDataFetcher<SomeDataType>(scheduler, remoteDataFetcher, remoteDataFetchingReporter);
    }

    @Test(expected = IllegalStateException.class)
    public void shouldInitOnlyOnce() throws IOException {
        context.checking(new Expectations() {{
            oneOf(remoteDataFetcher).init();
            ignoring(remoteDataFetcher);
        }});
        scheduledRemoteDataFetcher.init();
        scheduledRemoteDataFetcher.init();
    }

    @Test
    public void shouldFetchDataOnDemand() {
        context.checking(new Expectations() {{
            oneOf(remoteDataFetcher).fetchNow();
        }});

        scheduledRemoteDataFetcher.fetchNow();
    }

    @Test
    public void shouldReportInitializationFromDisk() {
        context.checking(new Expectations() {{
            oneOf(remoteDataFetcher).hasInitializedFromDisk();
        }});

        scheduledRemoteDataFetcher.hasInitializedFromDisk();
    }

    @Test
    public void shouldReportErrorFetchingFromRemote() throws IOException {
        context.checking(new Expectations() {{
            allowing(remoteDataFetchingReporter).attemptingToFetchFromRemote();
            allowing(remoteDataFetcher).hasInitializedFromDisk();
            will(returnValue(true));
            Exception e = new RuntimeException("IRRELEVANT");
            allowing(remoteDataFetcher).fetchNow();
            will(throwException(e));
            ignoring(remoteDataFetcher);
            oneOf(remoteDataFetchingReporter).cannotCompleteFetchingFromRemote(e);
        }});
        scheduledRemoteDataFetcher.init();
        scheduler.tick(1, TimeUnit.NANOSECONDS);
    }

    @Test
    public void shouldReportRemoteFetchingAttempt() {
        context.checking(new Expectations() {{
            allowing(remoteDataFetcher).hasInitializedFromDisk();
            will(returnValue(true));
            ignoring(remoteDataFetcher);
            oneOf(remoteDataFetchingReporter).attemptingToFetchFromRemote();
        }});

        scheduledRemoteDataFetcher.init();
        scheduler.tick(1, TimeUnit.NANOSECONDS);
    }

    @Test
    public void shouldInitWithNoDelayIfNoInitFromRemote() {
        context.checking(new Expectations() {{
            allowing(remoteDataFetcher).hasInitializedFromRemote();
            will(returnValue(false));
            oneOf(remoteDataFetcher).fetchNow();
            ignoring(remoteDataFetcher);
            ignoring(remoteDataFetchingReporter);
        }});
        scheduledRemoteDataFetcher.init();
        scheduler.tick(1, TimeUnit.NANOSECONDS);

    }

    @Test
    public void shouldSyncWithRemoteEveryMinuteWhenFailingToInit() {
        context.checking(new Expectations() {{
            final States scheduledResilientCacheState = context.states("scheduled resilient cache").startsAs("pre init");
            allowing(remoteDataFetcher).hasInitializedFromRemote();
            will(returnValue(false));
            oneOf(remoteDataFetcher).fetchNow();
            when(scheduledResilientCacheState.is("pre init"));
            then(scheduledResilientCacheState.is("post init"));
            oneOf(remoteDataFetcher).fetchNow();
            when(scheduledResilientCacheState.is("post init"));
            then(scheduledResilientCacheState.is("running"));
            ignoring(remoteDataFetcher);
            ignoring(remoteDataFetchingReporter);
        }});
        scheduledRemoteDataFetcher.init();
        scheduler.tick(1, TimeUnit.NANOSECONDS);
        scheduler.tick(1, TimeUnit.MINUTES);

    }

    @Test
    public void shouldSyncWithRemoteEveryFiveMinutesAfterSyncingWithRemote() {
        context.checking(new Expectations() {{
            allowing(remoteDataFetcher).hasInitializedFromRemote();
            will(returnValue(false));
            final States scheduledResilientCacheState = context.states("scheduled resilient cache").startsAs("pre init");
            oneOf(remoteDataFetcher).fetchNow();
            will(throwException(new RuntimeException("error")));
            when(scheduledResilientCacheState.is("pre init"));
            then(scheduledResilientCacheState.is("after error"));
            oneOf(remoteDataFetcher).fetchNow();
            when(scheduledResilientCacheState.is("after error"));
            then(scheduledResilientCacheState.is("reschedule"));
            oneOf(remoteDataFetcher).fetchNow();
            when(scheduledResilientCacheState.is("reschedule"));
            then(scheduledResilientCacheState.is("after reschedule"));
            oneOf(remoteDataFetcher).fetchNow();
            when(scheduledResilientCacheState.is("after reschedule"));
            allowing(remoteDataFetcher).init();
            allowing(remoteDataFetcher).hasInitializedFromDisk();
            ignoring(remoteDataFetchingReporter);
        }});
        scheduledRemoteDataFetcher.init();
        scheduler.tick(1, TimeUnit.NANOSECONDS);
        scheduler.tick(1, TimeUnit.MINUTES);
        scheduler.tick(5, TimeUnit.MINUTES);
    }

    @Test
    public void shutsDownTheScheduledExecutorServiceOnStop() {
        context.checking(new Expectations() {{
                ignoring(remoteDataFetcher);
                ignoring(remoteDataFetchingReporter);
        }});
        scheduledRemoteDataFetcher.init();
        scheduledRemoteDataFetcher.close();
        assertThat(scheduler, wasShutdown());
    }

    private Matcher<ScheduledExecutorService> wasShutdown() {
        return new TypeSafeDiagnosingMatcher<ScheduledExecutorService>() {
            @Override
            protected boolean matchesSafely(final ScheduledExecutorService item, final Description mismatchDescription) {
                mismatchDescription.appendText("shutdown flag was").appendValue(item.isShutdown());
                return item.isShutdown();
            }

            @Override
            public void describeTo(final Description description) {
                description.appendText("shutdown flag true");
            }
        };
    }

}
