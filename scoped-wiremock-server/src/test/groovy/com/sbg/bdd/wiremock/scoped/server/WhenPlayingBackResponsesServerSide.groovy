package com.sbg.bdd.wiremock.scoped.server

import com.sbg.bdd.wiremock.scoped.common.WhenPlayingBackResponsesCommon
import com.sbg.bdd.wiremock.scoped.server.junit.ScopedWireMockServerRule
import org.junit.rules.TestRule

class WhenPlayingBackResponsesServerSide extends WhenPlayingBackResponsesCommon {
    @Override
    protected TestRule createWireMockRule() {
        return new ScopedWireMockServerRule();
    }
}
