package com.sbg.bdd.wiremock.scoped.server;

import com.sbg.bdd.wiremock.scoped.server.junit.ScopedWireMockServerRule;
import org.junit.rules.TestRule;

public class WhenWorkingOutsideOfAnyScope extends com.sbg.bdd.wiremock.scoped.WhenWorkingOutsideOfAnyScope {

    protected TestRule createWireMockRule() {
        return new ScopedWireMockServerRule();
    }

}
