package com.sbg.bdd.wiremock.scoped.client

import com.sbg.bdd.wiremock.scoped.common.WhenRecordingExchangesRecursivelyCommon
import org.junit.rules.TestRule

class WhenRecordingExchangesRecursivelyClientSide extends WhenRecordingExchangesRecursivelyCommon{
    protected TestRule createWireMockRule() {
        return new ScopedWireMockClientRule(WireMockServerFactory.createAndReturnPort(),false);
    }
}
