package com.sbg.bdd.wiremock.scoped.client

import com.google.common.io.Files
import com.sbg.bdd.resource.file.DirectoryResourceRoot
import com.sbg.bdd.wiremock.scoped.admin.ScopedAdmin
import com.sbg.bdd.wiremock.scoped.common.WhenPlayingBackResponsesInScopeCommon
import com.sbg.bdd.wiremock.scoped.server.junit.ScopedWireMockServerRule
import org.junit.rules.TestRule

class WhenPlayingBackResponsesInScopeClientSide extends WhenPlayingBackResponsesInScopeCommon{
    @Override
    protected TestRule createWireMockRule() {
        def server = WireMockServerFactory.createAndReturnServer()
        def client = new ScopedHttpAdminClient('localhost', server.port())
        def clientRule = new ScopedWireMockClientRule(client)
        client.registerResourceRoot('root',server.getResourceRoot('root'))
        client.registerResourceRoot(ScopedAdmin.OUTPUT_RESOURCE_ROOT,server.getResourceRoot(ScopedAdmin.OUTPUT_RESOURCE_ROOT))
        client.registerResourceRoot(ScopedAdmin.JOURNAL_RESOURCE_ROOT,server.getResourceRoot(ScopedAdmin.JOURNAL_RESOURCE_ROOT))

        return clientRule
    }
}
