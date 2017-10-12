package com.sbg.bdd.wiremock.scoped.server

import com.sbg.bdd.wiremock.scoped.common.WhenManagingScopedMappingsCommon
import com.sbg.bdd.wiremock.scoped.server.junit.ScopedWireMockServerRule
import org.junit.rules.TestRule

class WhenManagingScopedMappingsServerSide extends WhenManagingScopedMappingsCommon{

    @Override
    protected TestRule createWireMockRule() {
        return new ScopedWireMockServerRule();
    }
}
