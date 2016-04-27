package com.wix.hoopoe.koboshi.cache.persistence;

import com.wix.hoopoe.koboshi.cache.TimestampedData;
import com.wix.hoopoe.koboshi.example.SomeDataType;
import com.wix.hoopoe.koboshi.marshaller.Marshaller;
import com.wix.hoopoe.koboshi.report.PersistenceException;
import org.hamcrest.CoreMatchers;
import org.jmock.Expectations;
import org.jmock.auto.Mock;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.joda.time.Instant;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;

public class DiskCacheTest {
    @Rule
    public JUnitRuleMockery context = new JUnitRuleMockery();

    @Mock
    private Marshaller<SomeDataType> marshaller;

    @Rule
    public TemporaryFolder disk = new TemporaryFolder();

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void shouldThrowPersistenceExceptionWhenWriteToDiskFails() {
        final Path diskCache = disk.getRoot().toPath();
        expectPersistenceExceptionWithIOCauseAndMessageFor(diskCache);
        DiskCache<SomeDataType> cache = new DiskCache<>(diskCache, marshaller);
        context.checking(new Expectations() {{
            ignoring(marshaller);
        }});

        cache.write(new TimestampedData<>(new SomeDataType("IRRELEVANT"), new Instant()));
    }

    @Test
    public void shouldThrowPersistenceExceptionWhenReadFromDiskFails() throws IOException {
        final byte[] content = "IRRELEVANT".getBytes();
        final Path someFile = aFileWith(content);
        expectPersistenceExceptionWithIOCauseAndMessageFor(someFile);
        DiskCache<SomeDataType> cache = new DiskCache<>(someFile, marshaller);
        context.checking(new Expectations() {{
            allowing(marshaller).unmarshall(content);
            will(throwException(new IOException()));
        }});

        cache.readTimestamped();
    }

    @Test
    public void shouldReturnNullIfFileDoesNotExist() {
        Path nonExistentFile = new File(disk.getRoot(), "NON EXISTENT").toPath();
        DiskCache<SomeDataType> cache = new DiskCache<>(nonExistentFile, marshaller);
        context.checking(new Expectations() {{
            ignoring(marshaller);
        }});

        assertThat(cache.readTimestamped(), nullValue());
    }

    private Path aFileWith(byte[] content) throws IOException {
        final Path someFile = disk.newFile().toPath();
        Files.write(someFile, content);
        return someFile;
    }

    private void expectPersistenceExceptionWithIOCauseAndMessageFor(Path someFile) {
        exception.expect(PersistenceException.class);
        exception.expectMessage(containsString(someFile.toString()));
        exception.expectCause(CoreMatchers.<Exception>instanceOf(IOException.class));
    }
}
