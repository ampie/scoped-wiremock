package com.sbg.bdd.wiremock.scoped.client

import com.sbg.bdd.wiremock.scoped.common.WhenManagingScopedMappingsCommon
import org.junit.rules.TestRule

class WhenManagingScopedMappingsClientSide extends WhenManagingScopedMappingsCommon {
    @Override
    protected TestRule createWireMockRule() {
        return new ScopedWireMockClientRule(WireMockServerFactory.createAndReturnPort(),false);
    }
}
