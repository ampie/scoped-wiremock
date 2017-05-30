package com.sbg.bdd.wiremock.scoped.server;

import com.sbg.bdd.wiremock.scoped.server.junit.ScopedWireMockServerRule;
import org.junit.rules.TestRule;

public class WhenManagingScopedMappings extends com.sbg.bdd.wiremock.scoped.WhenManagingScopedMappings {
    protected TestRule createWireMockRule() {
        return new ScopedWireMockServerRule();
    }


}
