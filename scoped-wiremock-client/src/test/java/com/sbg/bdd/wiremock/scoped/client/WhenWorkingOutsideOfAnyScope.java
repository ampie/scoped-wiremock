package com.sbg.bdd.wiremock.scoped.client;

import org.junit.rules.TestRule;

public class WhenWorkingOutsideOfAnyScope extends com.sbg.bdd.wiremock.scoped.WhenWorkingOutsideOfAnyScope {
    protected TestRule createWireMockRule() {
        return new ScopedWireMockClientRule(WireMockServerFactory.createAndReturnPort(),false);
    }

}
