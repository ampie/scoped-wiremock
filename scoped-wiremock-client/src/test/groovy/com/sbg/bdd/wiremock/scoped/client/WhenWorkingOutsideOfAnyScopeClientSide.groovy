package com.sbg.bdd.wiremock.scoped.client

import com.sbg.bdd.wiremock.scoped.common.WhenWorkingOutsideOfAnyScopeCommon
import org.junit.rules.TestRule

class WhenWorkingOutsideOfAnyScopeClientSide extends WhenWorkingOutsideOfAnyScopeCommon{
    @Override
    protected TestRule createWireMockRule() {
        return new ScopedWireMockClientRule(WireMockServerFactory.createAndReturnPort(),false);
    }
}
