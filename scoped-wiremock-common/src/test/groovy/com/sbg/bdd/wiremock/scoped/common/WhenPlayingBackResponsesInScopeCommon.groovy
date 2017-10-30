package com.sbg.bdd.wiremock.scoped.common

import com.github.tomakehurst.wiremock.common.HttpClientUtils
import com.github.tomakehurst.wiremock.http.HttpClientFactory
import com.github.tomakehurst.wiremock.http.RequestMethod
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder
import com.sbg.bdd.resource.ResourceSupport
import com.sbg.bdd.wiremock.scoped.admin.ScopedAdmin
import com.sbg.bdd.wiremock.scoped.admin.model.ExtendedRequestPattern
import com.sbg.bdd.wiremock.scoped.admin.model.ExtendedStubMapping
import com.sbg.bdd.wiremock.scoped.admin.model.GlobalCorrelationState
import com.sbg.bdd.wiremock.scoped.admin.model.JournalMode
import com.sbg.bdd.wiremock.scoped.admin.model.RecordingSpecification
import com.sbg.bdd.wiremock.scoped.integration.HeaderName
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpGet

import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching

abstract class WhenPlayingBackResponsesInScopeCommon extends ScopedWireMockCommonTest {
    def 'Should play back the example_journal directory as specified by a recording mapping in the global scope as per the global journal mode'() {
        given:'I have a GLobal Scope with the JournalMode set to playback'
        def globalCorrelationState = new GlobalCorrelationState('someRun', new URL(wireMock.baseUrl()), new URL(wireMock.baseUrl() + '/sut'), 'sutx')
        globalCorrelationState.globalJournaMode=JournalMode.PLAYBACK
        def globalScope = wireMock.startNewGlobalScope(globalCorrelationState)

        and: 'I have not setup any services, but I have prepared a journal from the example_journal directory for a service at /entry_point'
        ResourceSupport.copy(getDirectoryResourceRoot(),wireMock.getResourceRoot(ScopedAdmin.JOURNAL_RESOURCE_ROOT))

        and: 'I have registered a global RecordingSpecification to save recordings to the subdirectory "example_journal " in the journal|Root'
        def requestPattern = new ExtendedRequestPattern(globalScope.correlationPath, new RequestPatternBuilder(RequestMethod.GET, urlMatching("/.*")).build())
        def recordingSpecification = new RecordingSpecification()
        recordingSpecification.mapsToJournalDirectory('example_journal')
        def stubMapping = new ExtendedStubMapping(requestPattern, null)
        stubMapping.recordingSpecification=recordingSpecification
        wireMock.register(stubMapping)

        and: 'I have a nested scope containing a user scope for John Smith'
        def nestedRecordingScope = wireMock.startNestedScope(globalScope.correlationPath, 'nested1_recording_scope', Collections.emptyMap())
        def userScope= wireMock.startUserScope(nestedRecordingScope.correlationPath, 'John_Smith',Collections.emptyMap())

        when: 'I invoke a  service twice'
        def response0 = sendGet("/entry_point", userScope.correlationPath, 0)
        def response1 = sendGet("/entry_point", userScope.correlationPath, 1)

        then:'I see the responses previously recorded in the correct sequence'
        response0 == 'hello 0'
        response1 == 'hello 1'
    }

    private String sendGet(String path, String scopePath, int sequenceNumber) throws IOException {
        HttpGet get = new HttpGet("http://localhost:" + wireMock.port() + path)
        get.setHeader(HeaderName.ofTheCorrelationKey(), scopePath)
        get.setHeader(HeaderName.ofTheSequenceNumber(), sequenceNumber + '')
        CloseableHttpResponse response = HttpClientFactory.createClient().execute(get)
        return HttpClientUtils.getEntityAsStringAndCloseStream(response)
    }

}
