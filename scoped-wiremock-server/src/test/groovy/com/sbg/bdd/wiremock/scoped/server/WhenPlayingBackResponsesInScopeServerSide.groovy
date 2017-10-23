package com.sbg.bdd.wiremock.scoped.server

import com.google.common.io.Files
import com.sbg.bdd.resource.file.DirectoryResourceRoot
import com.sbg.bdd.wiremock.scoped.admin.ScopedAdmin
import com.sbg.bdd.wiremock.scoped.common.WhenPlayingBackResponsesInScopeCommon
import com.sbg.bdd.wiremock.scoped.server.junit.ScopedWireMockServerRule
import org.junit.rules.TestRule

class WhenPlayingBackResponsesInScopeServerSide  extends WhenPlayingBackResponsesInScopeCommon{
    @Override
    protected TestRule createWireMockRule() {
        def rule = new ScopedWireMockServerRule()
        rule.registerResourceRoot(ScopedAdmin.OUTPUT_RESOURCE_ROOT, new DirectoryResourceRoot(ScopedAdmin.OUTPUT_RESOURCE_ROOT, Files.createTempDir()))
        rule.registerResourceRoot(ScopedAdmin.PERSONA_RESOURCE_ROOT, getDirectoryResourceRoot().getChild('personas'))
        rule.registerResourceRoot(ScopedAdmin.JOURNAL_RESOURCE_ROOT, new DirectoryResourceRoot(ScopedAdmin.JOURNAL_RESOURCE_ROOT, Files.createTempDir()))
        return rule;
    }
}
