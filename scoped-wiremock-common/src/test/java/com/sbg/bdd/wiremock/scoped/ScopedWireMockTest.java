package com.sbg.bdd.wiremock.scoped;

import com.sbg.bdd.wiremock.scoped.common.HasBaseUrl;
import org.junit.Rule;
import org.junit.rules.TestRule;

public abstract class ScopedWireMockTest {
    @Rule
    public TestRule wireMockRule = createWireMockRule();

    protected abstract TestRule createWireMockRule();

    public ScopedWireMock getWireMock() {
        return (ScopedWireMock) wireMockRule;
    }

    public Integer getWireMockPort() {
        return ((HasBaseUrl) wireMockRule).port();
    }
}
