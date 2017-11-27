package com.sbg.bdd.wiremock.scoped.common

import com.github.tomakehurst.wiremock.common.HttpClientUtils
import com.github.tomakehurst.wiremock.http.HttpClientFactory
import com.github.tomakehurst.wiremock.http.RequestMethod
import com.github.tomakehurst.wiremock.matching.RequestPattern
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder
import com.sbg.bdd.resource.ResourceSupport
import com.sbg.bdd.wiremock.scoped.admin.ScopedAdmin
import com.sbg.bdd.wiremock.scoped.admin.model.ExtendedRequestPattern
import com.sbg.bdd.wiremock.scoped.admin.model.ExtendedStubMapping
import com.sbg.bdd.wiremock.scoped.admin.model.GlobalCorrelationState
import com.sbg.bdd.wiremock.scoped.admin.model.JournalMode
import com.sbg.bdd.wiremock.scoped.admin.model.RecordingSpecification
import com.sbg.bdd.wiremock.scoped.integration.HeaderName
import com.sbg.bdd.wiremock.scoped.integration.RuntimeCorrelationState
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpGet

import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching

abstract class WhenPlayingBackResponsesInScopeCommon extends ScopedWireMockCommonTest {
    def 'Should play back the example_journal directory as specified by a journal mapping in the global scope as per the global journal mode'() {
        //TODO split into two test cases: one reflecting the globally applied rule and one reflecting exact matching of correlationPAths
        given: 'I have a GLobal Scope with the JournalMode set to playback'
        def globalCorrelationState = new GlobalCorrelationState('someRun', new URL(wireMock.baseUrl()), new URL(wireMock.baseUrl() + '/sut'), 'sutx')
        globalCorrelationState.globalJournaMode = JournalMode.PLAYBACK
        def globalScope = wireMock.startNewGlobalScope(globalCorrelationState)

        and: 'I have not setup any services, but I have prepared a journal from the example_journal directory for a service at /entry_point'
        ResourceSupport.copy(getDirectoryResourceRoot(), wireMock.getResourceRoot(ScopedAdmin.JOURNAL_RESOURCE_ROOT))

        and: 'I have registered a global RecordingSpecification to save recordings to the subdirectory "example_journal " in the journal|Root'
        def requestPattern = new ExtendedRequestPattern(globalScope.correlationPath, new RequestPatternBuilder(RequestMethod.GET, urlMatching("/.*")).build())
        def recordingSpecification = new RecordingSpecification()
        recordingSpecification.mapsToJournalDirectory('example_journal')
        def stubMapping = new ExtendedStubMapping(requestPattern, null)
        stubMapping.recordingSpecification = recordingSpecification
        wireMock.register(stubMapping)

        and: 'I have a nested scope containing a user scope for John Smith with matching recordings'
        def nestedRecordingScope = wireMock.startNestedScope(globalScope.correlationPath, 'nested1_recording_scope', Collections.emptyMap())
        def nestedUserScope = wireMock.startUserScope(nestedRecordingScope.correlationPath, 'John_Smith', Collections.emptyMap())

        and: 'I have another nested scope one level deeper containing a user scope for John Smith with no matching recordings'
        def unrecordedScope = wireMock.startNestedScope(nestedRecordingScope.correlationPath, 'nested1_1_recording_scope', Collections.emptyMap())
        def unrecordedNestedUserScope = wireMock.startUserScope(unrecordedScope.correlationPath, 'John_Smith', Collections.emptyMap())

        when: 'I invoke a service twice at both levels'
        def response0 = sendGet("/entry_point", nestedUserScope.correlationPath, 0)
        def response1 = sendGet("/entry_point", nestedUserScope.correlationPath, 1)
        def response2 = sendGet("/entry_point", unrecordedNestedUserScope.correlationPath, 0)
        def response3 = sendGet("/entry_point", unrecordedNestedUserScope.correlationPath, 1)

        then: 'I see the responses previously recorded in the correct sequence at the higher level'
        response0 == 'hello 0'
        response1 == 'hello 1'
        and: 'no responses for requests at the lower level because the playback needs to be accurate'
        response2.contains('Request was not matched')
        response3.contains('Request was not matched')
    }

    def 'Should play back the example_recordings directory as specified by a playback mapping in the global scope'() {
        given: 'I have a GLobal Scope with the JournalMode set to playback'
        def globalCorrelationState = new GlobalCorrelationState('someRun', new URL(wireMock.baseUrl()), new URL(wireMock.baseUrl() + '/sut'), 'sutx')
        def globalScope = wireMock.startNewGlobalScope(globalCorrelationState)

        and: 'I have not setup any services, but I have prepared a journal from the example_journal directory for a service at /entry_point'
        ResourceSupport.copy(getDirectoryResourceRoot().getChild('personas'), wireMock.getResourceRoot(ScopedAdmin.PERSONA_RESOURCE_ROOT))

        and: 'I have registered a global RecordingSpecification to save recordings to the subdirectory "example_recordings" in the inputRoot'
        def requestPattern = new ExtendedRequestPattern(globalScope.correlationPath, new RequestPatternBuilder(RequestMethod.GET, urlMatching("/.*")).build())
        def recordingSpecification = new RecordingSpecification()
        recordingSpecification.playbackResponsesFrom('example_recordings')
        def stubMapping = new ExtendedStubMapping(requestPattern, null)
        stubMapping.recordingSpecification = recordingSpecification
        wireMock.register(stubMapping)

        and: 'I have a nested scope containing a user scope for John Smith with matching recordings'
        def nestedRecordingScope = wireMock.startNestedScope(globalScope.correlationPath, 'nested1_recording_scope', Collections.emptyMap())
        def nestedUserScope = wireMock.startUserScope(nestedRecordingScope.correlationPath, 'John_Smith', Collections.emptyMap())

        and: 'I have another nested scope one level deeper containing a user scope for John Smith with no matching recordings'
        def unrecordedScope = wireMock.startNestedScope(nestedRecordingScope.correlationPath, 'nested1_1_recording_scope', Collections.emptyMap())
        def unrecordedNestedUserScope = wireMock.startUserScope(unrecordedScope.correlationPath, 'John_Smith', Collections.emptyMap())

        when: 'I invoke a  service twice at both levels'
        def response0 = sendGet("/entry_point", nestedUserScope.correlationPath, 0)
        def response1 = sendGet("/entry_point", nestedUserScope.correlationPath, 1)
        def response2 = sendGet("/entry_point", unrecordedNestedUserScope.correlationPath, 0)
        def response3 = sendGet("/entry_point", unrecordedNestedUserScope.correlationPath, 1)

        then: 'I see the responses previously recorded in the correct sequence at the higher level'
        response0 == 'hello 0'
        response1 == 'hello 1'
        and: 'the same responses at the lower level because the playback should work at different levels '
        response2 == 'hello 0'
        response3 == 'hello 1'
    }

    def 'Should play back the example_template_journal directory and substitute template variables registered at the nested scope'() {
        given: 'I have a Global Scope with the JournalMode set to playback'
        def globalCorrelationState = new GlobalCorrelationState('someRun', new URL(wireMock.baseUrl()), new URL(wireMock.baseUrl() + '/sut'), 'sutx')
        globalCorrelationState.globalJournaMode = JournalMode.PLAYBACK
        def globalScope = wireMock.startNewGlobalScope(globalCorrelationState)

        and: 'I have registered a template variable "username" with value "johnnie" for John Smith at the Global Scope level'
        def globalUserScope = wireMock.startUserScope(globalScope.correlationPath, 'John_Smith', Collections.emptyMap())
        wireMock.registerTemplateVariables(globalUserScope.correlationPath, Collections.singletonMap('username', 'johnnie'))

        and: 'I have not setup any services, but I have prepared a journal from the example_template_journal directory for a service at /entry_point'
        ResourceSupport.copy(getDirectoryResourceRoot(), wireMock.getResourceRoot(ScopedAdmin.JOURNAL_RESOURCE_ROOT))

        and: 'I have registered a global RecordingSpecification to save recordings to the subdirectory "example_template_journal " in the journal|Root'
        def requestPattern = new ExtendedRequestPattern(globalScope.correlationPath, new RequestPatternBuilder(RequestMethod.GET, urlMatching("/.*")).build())
        def recordingSpecification = new RecordingSpecification()
        recordingSpecification.mapsToJournalDirectory('example_template_journal')
        def stubMapping = new ExtendedStubMapping(requestPattern, null)
        stubMapping.recordingSpecification = recordingSpecification
        wireMock.register(stubMapping)

        and: 'I have a nested scope containing a user scope for John Smith'
        def nestedRecordingScope = wireMock.startNestedScope(globalScope.correlationPath, 'nested1_recording_scope', Collections.emptyMap())
        def userScope = wireMock.startUserScope(nestedRecordingScope.correlationPath, 'John_Smith', Collections.emptyMap())


        when: 'I invoke a  service twice'
        def response0 = sendGet("/entry_point", userScope.correlationPath, 0)
        def response1 = sendGet("/entry_point", userScope.correlationPath, 1)

        then: 'I see the responses previously recorded in the correct sequence, with the variable "username" substituted with "johnnie"'
        response0 == 'hello johnnie 0'
        response1 == 'hello johnnie 1'

    }

    private String sendGet(String path, String scopePath, int sequenceNumber) throws IOException {
        HttpGet get = new HttpGet("http://localhost:" + wireMock.port() + path)
        get.setHeader(HeaderName.ofTheCorrelationKey(), scopePath)
        get.setHeader(HeaderName.ofTheSequenceNumber(), sequenceNumber + '')
        get.setHeader(HeaderName.ofTheThreadContextId(), '1')
        CloseableHttpResponse response = HttpClientFactory.createClient().execute(get)
        return HttpClientUtils.getEntityAsStringAndCloseStream(response)
    }

}
