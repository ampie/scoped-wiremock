package com.sbg.bdd.wiremock.scoped.client

import com.sbg.bdd.wiremock.scoped.common.WhenPlayingBackResponsesCommon
import org.junit.rules.TestRule

class WhenPlayingBackResponsesClientSide extends WhenPlayingBackResponsesCommon{
    protected TestRule createWireMockRule() {
        return new ScopedWireMockClientRule(WireMockServerFactory.createAndReturnPort(),false);
    }
}
