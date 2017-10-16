package com.sbg.bdd.wiremock.scoped.client

import com.sbg.bdd.wiremock.scoped.common.WhenInheritingAndOverridingRulesCommon
import org.junit.rules.TestRule

class WhenInheritingAndOverridingRulesClientSide extends  WhenInheritingAndOverridingRulesCommon{
    @Override
    protected TestRule createWireMockRule() {
        return new ScopedWireMockClientRule(WireMockServerFactory.createAndReturnPort(),false);
    }
}
