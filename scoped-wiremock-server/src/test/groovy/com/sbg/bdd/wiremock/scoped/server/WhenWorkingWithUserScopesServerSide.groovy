package com.sbg.bdd.wiremock.scoped.server

import com.sbg.bdd.wiremock.scoped.common.WhenWorkingWithUserScopesCommon
import com.sbg.bdd.wiremock.scoped.server.junit.ScopedWireMockServerRule
import org.junit.rules.TestRule

class WhenWorkingWithUserScopesServerSide extends WhenWorkingWithUserScopesCommon {
    @Override
    protected TestRule createWireMockRule() {
        return new ScopedWireMockServerRule();
    }
}
