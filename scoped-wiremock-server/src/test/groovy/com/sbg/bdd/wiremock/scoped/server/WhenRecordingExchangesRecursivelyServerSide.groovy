package com.sbg.bdd.wiremock.scoped.server

import com.sbg.bdd.wiremock.scoped.common.WhenRecordingExchangesRecursivelyCommon
import com.sbg.bdd.wiremock.scoped.server.junit.ScopedWireMockServerRule
import org.junit.rules.TestRule

class WhenRecordingExchangesRecursivelyServerSide extends WhenRecordingExchangesRecursivelyCommon {
    @Override
    protected TestRule createWireMockRule() {
        return new ScopedWireMockServerRule();
    }
}