package com.sbg.bdd.wiremock.scoped.common

import com.github.tomakehurst.wiremock.client.WireMock
import com.sbg.bdd.resource.ResourceContainer
import com.sbg.bdd.resource.file.DirectoryResourceRoot
import com.sbg.bdd.wiremock.scoped.integration.HeaderName

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo

abstract class WhenPlayingBackResponsesCommon extends ScopedWireMockCommonTest {
    def 'should create a mapping for each non header file in the resource directory'() {
        given: 'a resource directory with 2 mapping files and 2 header files'
        DirectoryResourceRoot root = getDirectoryResourceRoot();
        def someRecordingDir = (ResourceContainer) root.resolveExisting("some_recording_dir")
        def stubMapping = WireMock.put("/context/service/operation").withHeader(HeaderName.ofTheCorrelationKey(), equalTo("my-correlation-key")).build()
        when: 'I instruct WireMock to serve the record mappings using a template stubMapping'
        wireMock.serveRecordedMappingsAt(someRecordingDir, stubMapping.request, 4)
        then: 'exactly two stubMappings should be created'
        wireMock.allStubMappings().getMappings().size() == 2
    }

}
