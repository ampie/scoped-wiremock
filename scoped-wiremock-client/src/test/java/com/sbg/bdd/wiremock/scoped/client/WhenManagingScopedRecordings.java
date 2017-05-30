package com.sbg.bdd.wiremock.scoped.client;

import com.sbg.bdd.wiremock.scoped.ScopedWireMockClientRule;
import org.junit.rules.TestRule;

public class WhenManagingScopedRecordings extends com.sbg.bdd.wiremock.scoped.WhenManagingScopedRecordings {

    protected TestRule createWireMockRule() {
        return new ScopedWireMockClientRule(WireMockServerFactory.createAndReturnPort(),false);
    }

}
