package com.sbg.bdd.wiremock.scoped.recording

import com.github.tomakehurst.wiremock.core.Options
import com.sbg.bdd.wiremock.scoped.integration.HeaderName
import com.sbg.bdd.wiremock.scoped.server.ScopedWireMockServer

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo
import static com.github.tomakehurst.wiremock.http.RequestMethod.*
import static com.sbg.bdd.wiremock.scoped.recording.strategies.RequestStrategies.a

class WhenPlayingBackResponses extends WhenWorkingWithWireMock{

    def 'should create a mapping for each non-header file in the resource directory'() throws Exception{

        given:
        def recordingWireMock = new RecordingWireMockClient(new ScopedWireMockServer(Options.DYNAMIC_PORT))

        when:

        recordingWireMock.serveRecordedMappingsAt(
                WireMockContextStub.getSrcTestResources().resolveExisting('some_recording_dir'),
                a(PUT).to('/context/service/operation').withHeader(HeaderName.ofTheCorrelationKey(), equalTo( 'my-correlation-key')),
                4
        )

        then:
        def mappings =recordingWireMock.allStubMappings().mappings
        mappings.size() == 2
    }
}
