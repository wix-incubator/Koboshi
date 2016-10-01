package com.wix.hoopoe.koboshi.cache.persistence;

import com.wix.hoopoe.koboshi.cache.TimestampedData;
import com.wix.hoopoe.koboshi.example.SomeDataType;
import com.wix.hoopoe.koboshi.marshaller.Marshaller;
import com.wix.hoopoe.koboshi.report.DelegatingRemoteDataFetcherReporter;
import com.wix.hoopoe.koboshi.report.PersistenceException;
import com.wix.hoopoe.koboshi.report.RemoteDataFetchingReporter;
import org.hamcrest.CoreMatchers;
import org.jmock.Expectations;
import org.jmock.auto.Mock;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.joda.time.Instant;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import scala.collection.mutable.ArrayBuffer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;

public class DiskCacheTest {
    private static final TimestampedData<SomeDataType> IRRELEVANT =
            new TimestampedData<>(new SomeDataType("IRRELEVANT"), new Instant());
    @Rule
    public JUnitRuleMockery context = new JUnitRuleMockery();

    @Mock
    private Marshaller<SomeDataType> marshaller;

    @Mock
    private RemoteDataFetchingReporter reporter;

    @Rule
    public TemporaryFolder disk = new TemporaryFolder();

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void shouldThrowPersistenceExceptionWhenWriteToDiskFails() {
        final Path diskCache = disk.getRoot().toPath();
        expectPersistenceExceptionWithIOCauseAndMessageFor(diskCache);
        DiskCache<SomeDataType> cache = new DiskCache<>(diskCache, aDummyMarshaller(), aDummyReporter());
        cache.write(IRRELEVANT);
    }

    @Test
    public void shouldThrowPersistenceExceptionWhenReadFromDiskFails() throws IOException {
        final byte[] content = "IRRELEVANT".getBytes();
        final Path someFile = aFileWith(content);
        expectPersistenceExceptionWithIOCauseAndMessageFor(someFile);
        DiskCache<SomeDataType> cache = new DiskCache<>(someFile, marshaller, aDummyReporter());
        context.checking(new Expectations() {{
            allowing(marshaller).unmarshall(content);
            will(throwException(new IOException()));
        }});

        cache.readTimestamped();
    }

    @Test
    public void shouldReturnNullIfFileDoesNotExist() {
        Path nonExistentFile = new File(disk.getRoot(), "NON EXISTENT").toPath();
        DiskCache<SomeDataType> cache = new DiskCache<>(nonExistentFile, aDummyMarshaller(), aDummyReporter());
        assertThat(cache.readTimestamped(), nullValue());
    }

    @Test
    public void shouldReportAttemptingToRead() throws IOException {
        final byte[] content = "IRRELEVANT".getBytes();
        final Path someFile = aFileWith(content);
        DiskCache<SomeDataType> cache = new DiskCache<>(someFile, aDummyMarshaller(), reporter);
        context.checking(new Expectations() {{
            oneOf(reporter).readFromPersistentCache(someFile.toUri(), content);
        }});

        cache.readTimestamped();
    }


    @Test
    public void shouldReportAttemptingToWrite() throws IOException {
        final byte[] content = "IRRELEVANT".getBytes();
        final Path someFile = aFile();
        DiskCache<SomeDataType> cache = new DiskCache<>(someFile, marshaller, reporter);
        context.checking(new Expectations() {{
            allowing(marshaller).marshall(with(any(TimestampedData.class)));
            will(returnValue(content));
            oneOf(reporter).writeToPersistentCache(someFile.toUri(), content);
        }});

        cache.write(IRRELEVANT);
    }

    private DummyMarshaller aDummyMarshaller() {
        return new DummyMarshaller();
    }

    private Path aFileWith(byte[] content) throws IOException {
        final Path someFile = aFile();
        Files.write(someFile, content);
        return someFile;
    }

    private Path aFile() throws IOException {
        return disk.newFile().toPath();
    }

    private void expectPersistenceExceptionWithIOCauseAndMessageFor(Path someFile) {
        exception.expect(PersistenceException.class);
        exception.expectMessage(containsString(someFile.toString()));
        exception.expectCause(CoreMatchers.<Exception>instanceOf(IOException.class));
    }

    private RemoteDataFetchingReporter aDummyReporter() {
        return new DelegatingRemoteDataFetcherReporter(new ArrayBuffer<RemoteDataFetchingReporter>());
    }

    class DummyMarshaller implements Marshaller<SomeDataType> {

        @Override
        public TimestampedData<SomeDataType> unmarshall(final byte[] timestampedData) throws IOException {
            return null;
        }

        @Override
        public byte[] marshall(final TimestampedData<SomeDataType> timestampedData) throws IOException {
            return new byte[0];
        }
    }
}
