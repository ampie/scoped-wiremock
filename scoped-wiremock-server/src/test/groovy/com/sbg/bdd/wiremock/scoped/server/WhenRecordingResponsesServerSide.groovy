package com.sbg.bdd.wiremock.scoped.server

import com.google.common.io.Files
import com.sbg.bdd.resource.file.DirectoryResourceRoot
import com.sbg.bdd.wiremock.scoped.common.WhenRecordingResponsesCommon
import com.sbg.bdd.wiremock.scoped.server.junit.ScopedWireMockServerRule
import org.junit.rules.TestRule

class WhenRecordingResponsesServerSide extends WhenRecordingResponsesCommon {
    @Override
    protected TestRule createWireMockRule() {
        def rule = new ScopedWireMockServerRule()
        rule.registerResourceRoot('outputRoot', new DirectoryResourceRoot('outputRoot', Files.createTempDir()))
        return rule;
    }

}
