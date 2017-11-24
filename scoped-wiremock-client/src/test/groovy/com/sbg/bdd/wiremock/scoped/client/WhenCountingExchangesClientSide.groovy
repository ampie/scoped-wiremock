package com.sbg.bdd.wiremock.scoped.client

import com.sbg.bdd.wiremock.scoped.common.WhenCountingExchangesCommon
import com.sbg.bdd.wiremock.scoped.server.junit.ScopedWireMockServerRule
import org.junit.rules.TestRule

class WhenCountingExchangesClientSide extends WhenCountingExchangesCommon {
    @Override
    protected TestRule createWireMockRule() {
        return new ScopedWireMockClientRule(WireMockServerFactory.createAndReturnServer())
    }
}
