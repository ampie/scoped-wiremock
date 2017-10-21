package com.sbg.bdd.wiremock.scoped.common

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.http.RequestMethod
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder
import com.sbg.bdd.resource.ResourceContainer
import com.sbg.bdd.resource.file.DirectoryResourceRoot
import com.sbg.bdd.wiremock.scoped.admin.model.ExtendedRequestPattern
import com.sbg.bdd.wiremock.scoped.admin.model.GlobalCorrelationState
import com.sbg.bdd.wiremock.scoped.integration.HeaderName

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching

abstract class WhenPlayingBackResponsesCommon extends ScopedWireMockCommonTest {
    def 'should create a mapping for each non header file in the resource directory'() {
        given: 'a resource directory with 2 mapping files and 2 header files'
        def globalScope = wireMock.startNewGlobalScope(new GlobalCorrelationState('someRun', new URL(wireMock.baseUrl()), new URL(wireMock.baseUrl() + '/sut'), 'sutx'))
        def nestedScope = wireMock.joinCorrelatedScope(globalScope.correlationPath ,   'nestedScope', Collections.emptyMap())

        DirectoryResourceRoot root = getDirectoryResourceRoot();
        def someRecordingDir = (ResourceContainer) root.resolveExisting("some_recording_dir")
        def stubMapping =  WireMock.put("/context/service/operation").withHeader(HeaderName.ofTheCorrelationKey(), equalTo("my-correlation-key")).build()
        def requestPattern = new ExtendedRequestPattern(nestedScope.correlationPath, WireMock.put("/context/service/operation").withHeader(HeaderName.ofTheCorrelationKey(), equalTo("my-correlation-key")).build().request)
        when: 'I instruct WireMock to serve the record mappings using a template stubMapping'
        wireMock.serveRecordedMappingsAt(someRecordingDir, requestPattern, 4)
        then: 'exactly two stubMappings should be created'
        wireMock.allStubMappings().getMappings().size() == 2
    }

}
