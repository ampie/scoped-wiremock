package com.sbg.bdd.wiremock.scoped.server

import com.sbg.bdd.wiremock.scoped.common.WhenWorkingWithScopesCommon
import com.sbg.bdd.wiremock.scoped.server.junit.ScopedWireMockServerRule
import org.junit.rules.TestRule

class WhenWorkingWithScopesServerSide extends WhenWorkingWithScopesCommon{
    @Override
    protected TestRule createWireMockRule() {
        return new ScopedWireMockServerRule();
    }
}
