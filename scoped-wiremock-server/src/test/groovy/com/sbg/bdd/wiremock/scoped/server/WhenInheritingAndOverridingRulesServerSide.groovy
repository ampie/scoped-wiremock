package com.sbg.bdd.wiremock.scoped.server

import com.sbg.bdd.wiremock.scoped.common.WhenInheritingAndOverridingRulesCommon
import com.sbg.bdd.wiremock.scoped.server.junit.ScopedWireMockServerRule
import org.junit.rules.TestRule

class WhenInheritingAndOverridingRulesServerSide extends WhenInheritingAndOverridingRulesCommon{
    @Override
    protected TestRule createWireMockRule() {
        return new ScopedWireMockServerRule();
    }
}
