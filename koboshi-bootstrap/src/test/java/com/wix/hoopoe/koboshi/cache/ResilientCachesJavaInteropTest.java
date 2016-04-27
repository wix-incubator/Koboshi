package com.wix.hoopoe.koboshi.cache;

import com.wix.hoopoe.koboshi.remote.RemoteDataSource;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;

public class ResilientCachesJavaInteropTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void anInitializedResilientCacheCanEasilyBeCreatedFromJava() throws IOException {
        ResilientCaches resilientCaches = resilientCaches();
        resilientCaches.aResilientInitializedCache(String.class, someStringRemoteDataSource());
    }

    @Test
    public void aResilientCacheBuilderCanEasilyBeCreatedFromJava() throws IOException {
        ResilientCaches resilientCaches = resilientCaches();
        resilientCaches.aResilientCacheBuilder(String.class, someStringRemoteDataSource());
    }

    private RemoteDataSource<String> someStringRemoteDataSource() {
        return new RemoteDataSource<String>() {

            @Override
            public String fetch() throws IOException {
                return "IRRELEVANT";
            }
        };
    }

    private ResilientCaches resilientCaches() throws IOException {
        //TODO move this test to use a different factory since "defaults" should move to a different module
        return com.wix.hoopoe.koboshi.cache.defaults.ResilientCaches.resilientCaches(temporaryFolder.newFolder());
    }
}
