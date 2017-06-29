package com.sbg.bdd.wiremock.scoped.client;

import org.junit.rules.TestRule;

public class WhenRecordingExchangesRecursively extends com.sbg.bdd.wiremock.scoped.WhenRecordingExchangesRecursively {

    protected TestRule createWireMockRule() {
        return new ScopedWireMockClientRule(WireMockServerFactory.createAndReturnPort(),false);
    }

}
