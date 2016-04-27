package com.wix.hoopoe.koboshi.spec;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wix.hoopoe.koboshi.cache.CustomizableResilientCaches;
import com.wix.hoopoe.koboshi.marshaller.JacksonMarshallers;
import com.wix.hoopoe.koboshi.cache.ReadOnlyLocalCache;
import com.wix.hoopoe.koboshi.cache.persistence.FolderPersistentCaches;
import com.wix.hoopoe.koboshi.registry.MapBasedRemoteDataFetcherRegistry;
import com.wix.hoopoe.koboshi.remote.FakeRemoteDataSource;
import com.wix.hoopoe.koboshi.report.DelegatingRemoteDataFetcherReporter;
import com.wix.hoopoe.koboshi.report.FixedReporterReporters;
import com.wix.hoopoe.koboshi.report.RemoteDataFetchingReporter;
import com.wix.hoopoe.koboshi.scheduler.DeterministicSchedulers;
import com.wix.hoopoe.koboshi.scheduler.SystemClock;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import scala.collection.mutable.ArrayBuffer;
import scala.runtime.AbstractFunction0;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;

/**
 * @author ittaiz
 * @since 12/25/13
 */
public class TransientCacheSpecPersistenceIT {

    private static final String NAMESPACE = "someNamespace";
    private static final RemoteDataFetchingReporter NOP_REPORTER = new DelegatingRemoteDataFetcherReporter(new ArrayBuffer<RemoteDataFetchingReporter>());
    @Rule
    public TemporaryFolder disk = new TemporaryFolder();

    private File cacheDirectory;
    private FakeRemoteDataSource<String> fakeRemoteDataSource;

    @Before
    public void setUp() throws Exception {
        cacheDirectory = disk.newFolder();
        fakeRemoteDataSource = new FakeRemoteDataSource<>();
    }

    @Test
    public void createsFileWithNamespace() throws IOException {
        fakeRemoteDataSource.act(new AbstractFunction0<String>() {
            @Override
            public String apply() {
                return "someValue";
            }
        });
        aResilientInitializedCache(NAMESPACE, cacheDirectory, fakeRemoteDataSource);
        assertThat("Cache files in directory",
                Arrays.asList(cacheDirectory.list()),
                hasItem(containsString(NAMESPACE)));
    }

    private ReadOnlyLocalCache<String> aResilientInitializedCache(String namespace, final File cacheDirectory,
                                                                      FakeRemoteDataSource<String>
                                                                              fakeRemoteDataSource) {
        final ObjectMapper objectMapper = new ObjectMapper();
        final MapBasedRemoteDataFetcherRegistry remoteDataFetcherRegistry = new MapBasedRemoteDataFetcherRegistry();
        final CustomizableResilientCaches customizableResilientCaches = new CustomizableResilientCaches(remoteDataFetcherRegistry,
                //TODO: actual executor service
                new DeterministicSchedulers(),
                //TODO: log to logger
                new FixedReporterReporters(NOP_REPORTER),
                new FolderPersistentCaches(cacheDirectory),
                new JacksonMarshallers(objectMapper),
                new SystemClock());
        return customizableResilientCaches.aResilientCacheBuilder(String.class, fakeRemoteDataSource)
                .withCustomNamespace(namespace)
                .build();
    }

}
