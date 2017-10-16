package com.sbg.bdd.wiremock.scoped.client

import com.sbg.bdd.wiremock.scoped.common.WhenWorkingWithScopesCommon
import org.junit.rules.TestRule

class WhenWorkingWithScopesClientSide extends WhenWorkingWithScopesCommon{
    @Override
    protected TestRule createWireMockRule() {
        return new ScopedWireMockClientRule(WireMockServerFactory.createAndReturnPort(),false);
    }
}
