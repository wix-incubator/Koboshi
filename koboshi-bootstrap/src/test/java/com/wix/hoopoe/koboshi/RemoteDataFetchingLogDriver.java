package com.wix.hoopoe.koboshi;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.slf4j.Logger;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.junit.Assert.assertThat;

public class RemoteDataFetchingLogDriver {
    private final ListAppender<ILoggingEvent> inMemoryAppender;

    private RemoteDataFetchingLogDriver(Logger logger) {
        inMemoryAppender = new ListAppender<>();
        inMemoryAppender.start();
        ((ch.qos.logback.classic.Logger) logger).addAppender(inMemoryAppender);
        ((ch.qos.logback.classic.Logger) logger).setLevel(Level.TRACE);
    }

    public static RemoteDataFetchingLogDriver aRemoteDataFetchingLogDriver(Logger logger) {
        return new RemoteDataFetchingLogDriver(logger);
    }

    public void reports(Level messageLevel, Matcher<String> messageMatcher) {
        reports(messageLevel, messageMatcher, false);
    }

    public void reportsWithException(final Level error, final Matcher<String> messageMatcher) {
        reports(error, messageMatcher, true);
    }

    private void reports(final Level messageLevel, final Matcher<String> messageMatcher,
                         final boolean checkForException) {
        assertThat(inMemoryAppender.list, hasItem(new TypeSafeMatcher<ILoggingEvent>() {
            @Override
            public void describeTo(final Description description) {
                description.appendText("logEvent level ").appendValue(messageLevel).appendText(" and message ").appendDescriptionOf(messageMatcher);
                if (checkForException){
                    description.appendText(" and exception is mandatory");
                }
            }

            @Override
            protected boolean matchesSafely(final ILoggingEvent loggingEvent) {
                return  messageLevel.equals(loggingEvent.getLevel()) &&
                        messageMatcher.matches(loggingEvent.getFormattedMessage()) &&
                        (!checkForException || notNullValue().matches(loggingEvent.getThrowableProxy()));
            }
        }));
    }
}