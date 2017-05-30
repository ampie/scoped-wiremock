package com.sbg.bdd.wiremock.scoped.server;

import com.sbg.bdd.wiremock.scoped.server.junit.ScopedWireMockServerRule;
import org.junit.rules.TestRule;


public class WhenInheritingAndOverridingRules extends com.sbg.bdd.wiremock.scoped.WhenInheritingAndOverridingRules {

    protected TestRule createWireMockRule() {
        return new ScopedWireMockServerRule();
    }
}
