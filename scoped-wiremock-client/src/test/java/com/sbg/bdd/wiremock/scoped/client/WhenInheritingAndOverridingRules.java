package com.sbg.bdd.wiremock.scoped.client;

import org.junit.rules.TestRule;


public class WhenInheritingAndOverridingRules extends com.sbg.bdd.wiremock.scoped.WhenInheritingAndOverridingRules {

    protected TestRule createWireMockRule() {
        return new ScopedWireMockClientRule(WireMockServerFactory.createAndReturnPort(),true);
    }

}
