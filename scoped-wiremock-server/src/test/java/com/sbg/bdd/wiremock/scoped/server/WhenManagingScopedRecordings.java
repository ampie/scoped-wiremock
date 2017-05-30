package com.sbg.bdd.wiremock.scoped.server;

import com.sbg.bdd.wiremock.scoped.server.junit.ScopedWireMockServerRule;
import org.junit.rules.TestRule;

public class WhenManagingScopedRecordings extends com.sbg.bdd.wiremock.scoped.WhenManagingScopedRecordings {
    protected TestRule createWireMockRule() {
        return new ScopedWireMockServerRule();
    }

}
