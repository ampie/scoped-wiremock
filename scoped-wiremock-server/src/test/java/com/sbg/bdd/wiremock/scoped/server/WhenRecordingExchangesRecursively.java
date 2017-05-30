package com.sbg.bdd.wiremock.scoped.server;

import com.sbg.bdd.wiremock.scoped.server.junit.ScopedWireMockServerRule;
import org.junit.rules.TestRule;

public class WhenRecordingExchangesRecursively extends com.sbg.bdd.wiremock.scoped.WhenRecordingExchangesRecursively {

    protected TestRule createWireMockRule() {
        return new ScopedWireMockServerRule();
    }

}
