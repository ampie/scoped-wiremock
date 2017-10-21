package com.sbg.bdd.wiremock.scoped.client

import com.sbg.bdd.wiremock.scoped.common.WhenRetrievingExchangesCommon
import org.junit.rules.TestRule

class WhenRetrievingExchangesClientSide extends WhenRetrievingExchangesCommon{
    protected TestRule createWireMockRule() {
        return new ScopedWireMockClientRule(WireMockServerFactory.createAndReturnPort(),false);
    }
}
