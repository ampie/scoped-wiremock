package com.sbg.bdd.wiremock.scoped.client;


import com.sbg.bdd.wiremock.scoped.ScopedWireMockClientRule;
import com.sbg.bdd.wiremock.scoped.server.junit.ScopedWireMockServerRule;
import org.junit.rules.TestRule;

public class WhenPlayingBackResponses extends com.sbg.bdd.wiremock.scoped.WhenPlayingBackResponses {
    protected TestRule createWireMockRule() {
        return new ScopedWireMockClientRule(WireMockServerFactory.createAndReturnPort(),false);
    }

}
