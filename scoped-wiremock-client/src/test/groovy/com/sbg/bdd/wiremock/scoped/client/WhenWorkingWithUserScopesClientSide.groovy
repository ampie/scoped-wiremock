package com.sbg.bdd.wiremock.scoped.client

import com.sbg.bdd.wiremock.scoped.common.WhenWorkingWithUserScopesCommon
import com.sbg.bdd.wiremock.scoped.server.junit.ScopedWireMockServerRule
import org.junit.rules.TestRule

class WhenWorkingWithUserScopesClientSide extends WhenWorkingWithUserScopesCommon {
    @Override
    protected TestRule createWireMockRule() {
        return new ScopedWireMockClientRule(WireMockServerFactory.createAndReturnPort(),false);
    }
}
