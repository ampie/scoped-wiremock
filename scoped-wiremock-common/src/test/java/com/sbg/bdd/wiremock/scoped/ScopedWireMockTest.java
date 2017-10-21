package com.sbg.bdd.wiremock.scoped;

import com.github.tomakehurst.wiremock.common.Json;
import com.sbg.bdd.resource.file.DirectoryResourceRoot;
import com.sbg.bdd.wiremock.scoped.admin.model.CorrelationState;
import com.sbg.bdd.wiremock.scoped.common.HasBaseUrl;
import org.junit.Rule;
import org.junit.rules.TestRule;

import java.io.File;
import java.util.Collections;
import java.util.regex.Pattern;

public abstract class ScopedWireMockTest {
    @Rule
    public TestRule wireMockRule = createWireMockRule();

    public static DirectoryResourceRoot getDirectoryResourceRoot() {
        File marker = new File(Thread.currentThread().getContextClassLoader().getResource("scoped-wiremock-common-marker.txt").getFile());
        return new DirectoryResourceRoot("root", marker.getParentFile());
    }

    protected abstract TestRule createWireMockRule();

    public ScopedWireMock getWireMock() {
        return (ScopedWireMock) wireMockRule;
    }

    public Integer getWireMockPort() {
        return ((HasBaseUrl) wireMockRule).port();
    }


}
