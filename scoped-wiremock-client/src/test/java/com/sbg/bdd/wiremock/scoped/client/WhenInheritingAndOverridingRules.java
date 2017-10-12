package com.sbg.bdd.wiremock.scoped.client;

import com.sbg.bdd.wiremock.scoped.ScopedWireMockTest;
import org.junit.rules.TestRule;


public class WhenInheritingAndOverridingRules extends ScopedWireMockTest {

    protected TestRule createWireMockRule() {
        return new ScopedWireMockClientRule(WireMockServerFactory.createAndReturnPort(),true);
    }

}
