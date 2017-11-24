package com.sbg.bdd.wiremock.scoped.client

import com.sbg.bdd.wiremock.scoped.common.WhenRegisteringExtendedStubMappingsCommon
import org.junit.rules.TestRule

class WhenRegisteringExtendedStubMappingsClientSide extends WhenRegisteringExtendedStubMappingsCommon {
    protected TestRule createWireMockRule() {
        return new ScopedWireMockClientRule(WireMockServerFactory.createAndReturnServer());
    }
}
