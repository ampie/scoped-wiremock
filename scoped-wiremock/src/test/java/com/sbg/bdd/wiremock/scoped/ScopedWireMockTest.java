package com.sbg.bdd.wiremock.scoped;

import com.sbg.bdd.wiremock.scoped.client.ScopedWireMock;
import com.sbg.bdd.wiremock.scoped.junit.ScopedWireMockServerRule;
import org.junit.Rule;
import org.junit.rules.TestRule;

public abstract class ScopedWireMockTest {
    @Rule
    public TestRule wireMockRule = createWireMockRule();

    protected TestRule createWireMockRule() {
        //TODO parameterize somehow
        return new ScopedWireMockServerRule();
    }

    public ScopedWireMock getWireMock() {
        if (wireMockRule instanceof ScopedWireMockServerRule) {
            return ((ScopedWireMockServerRule) wireMockRule).getClient();
        } else {
            return (ScopedWireMock) wireMockRule;
        }
    }
    public Integer getWireMockPort() {
        if (wireMockRule instanceof ScopedWireMockServerRule) {
            return ((ScopedWireMockServerRule) wireMockRule).port();
        } else {
            return ((ScopedWireMock) wireMockRule).port();
        }
    }
}
