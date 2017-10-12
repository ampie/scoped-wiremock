package com.sbg.bdd.wiremock.scoped.common

import com.sbg.bdd.resource.file.DirectoryResourceRoot
import com.sbg.bdd.wiremock.scoped.ScopedWireMock
import org.junit.Rule
import org.junit.rules.TestRule
import spock.lang.Specification

abstract class ScopedWireMockCommonTest extends Specification{
    @Rule
    TestRule wireMockRule = createWireMockRule();

    static DirectoryResourceRoot getDirectoryResourceRoot() {
        File marker = new File(Thread.currentThread().getContextClassLoader().getResource("scoped-wiremock-common-marker.txt").getFile());
        return new DirectoryResourceRoot("root", marker.getParentFile());
    }

    protected abstract TestRule createWireMockRule();

    ScopedWireMock getWireMock() {
        return (ScopedWireMock) wireMockRule;
    }

    Integer getWireMockPort() {
        return ((HasBaseUrl) wireMockRule).port();
    }
}
