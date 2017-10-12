package com.sbg.bdd.wiremock.scoped.server

import com.sbg.bdd.wiremock.scoped.common.WhenRegisteringExtendedStubMappingsCommon
import com.sbg.bdd.wiremock.scoped.server.junit.ScopedWireMockServerRule
import org.junit.rules.TestRule

class WhenRegisteringExtendedStubMappingsServerSide extends WhenRegisteringExtendedStubMappingsCommon{
    @Override
    protected TestRule createWireMockRule() {
        return new ScopedWireMockServerRule();
    }
}
