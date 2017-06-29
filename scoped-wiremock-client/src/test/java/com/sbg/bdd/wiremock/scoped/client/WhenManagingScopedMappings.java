package com.sbg.bdd.wiremock.scoped.client;

import org.junit.rules.TestRule;

public class WhenManagingScopedMappings extends com.sbg.bdd.wiremock.scoped.WhenManagingScopedMappings {

    protected TestRule createWireMockRule() {
        return new ScopedWireMockClientRule(WireMockServerFactory.createAndReturnPort(),false);
    }

}
