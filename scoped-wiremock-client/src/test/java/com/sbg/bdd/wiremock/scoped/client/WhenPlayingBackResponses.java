package com.sbg.bdd.wiremock.scoped.client;


import com.sbg.bdd.wiremock.scoped.ScopedWireMockTest;
import org.junit.rules.TestRule;

public class WhenPlayingBackResponses extends ScopedWireMockTest {
    protected TestRule createWireMockRule() {
        return new ScopedWireMockClientRule(WireMockServerFactory.createAndReturnPort(),false);
    }

}
