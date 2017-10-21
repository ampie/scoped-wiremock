package com.sbg.bdd.wiremock.scoped.server

import com.sbg.bdd.wiremock.scoped.common.WhenRetrievingExchangesCommon
import com.sbg.bdd.wiremock.scoped.server.junit.ScopedWireMockServerRule
import org.junit.rules.TestRule

class WhenRetrievingExchangesServerSide extends WhenRetrievingExchangesCommon {
    @Override
    protected TestRule createWireMockRule() {
        return new ScopedWireMockServerRule();
    }
}