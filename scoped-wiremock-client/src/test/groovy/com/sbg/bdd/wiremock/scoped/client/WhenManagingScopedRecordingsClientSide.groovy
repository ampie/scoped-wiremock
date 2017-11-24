package com.sbg.bdd.wiremock.scoped.client

import com.sbg.bdd.wiremock.scoped.common.WhenManagingScopedRecordingsCommon
import org.junit.rules.TestRule

class WhenManagingScopedRecordingsClientSide extends WhenManagingScopedRecordingsCommon {

    protected TestRule createWireMockRule() {
        return new ScopedWireMockClientRule(WireMockServerFactory.createAndReturnServer())
    }
}
