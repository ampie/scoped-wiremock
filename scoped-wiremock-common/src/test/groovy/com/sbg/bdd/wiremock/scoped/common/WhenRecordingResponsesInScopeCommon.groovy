package com.sbg.bdd.wiremock.scoped.common

import com.github.tomakehurst.wiremock.common.HttpClientUtils
import com.github.tomakehurst.wiremock.http.HttpClientFactory
import com.github.tomakehurst.wiremock.http.RequestMethod
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder
import com.sbg.bdd.resource.Resource
import com.sbg.bdd.wiremock.scoped.admin.ScopedAdmin
import com.sbg.bdd.wiremock.scoped.admin.model.ExtendedRequestPattern
import com.sbg.bdd.wiremock.scoped.admin.model.ExtendedStubMapping
import com.sbg.bdd.wiremock.scoped.admin.model.GlobalCorrelationState
import com.sbg.bdd.wiremock.scoped.admin.model.JournalMode
import com.sbg.bdd.wiremock.scoped.admin.model.RecordingSpecification
import com.sbg.bdd.wiremock.scoped.integration.HeaderName
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpGet
import org.hamcrest.MatcherAssert
import org.hamcrest.core.IsEqual

import static com.github.tomakehurst.wiremock.client.WireMock.*
import static org.hamcrest.core.Is.is

abstract class WhenRecordingResponsesInScopeCommon extends ScopedWireMockCommonTest{
    def 'Should record all exchanges and nested exchanges in the directory specified by a recording mapping within a nested scope'() {
        given:'I have a nested scope containing a user scope for John Smith'
        def globalScope = wireMock.startNewGlobalScope(new GlobalCorrelationState('someRun', new URL(wireMock.baseUrl()), new URL(wireMock.baseUrl() + '/sut'), 'sutx'))
        def nestedRecordingScope = wireMock.joinCorrelatedScope(globalScope.correlationPath, 'nested1_recording_scope', Collections.emptyMap())
        def userScope= wireMock.joinUserScope(nestedRecordingScope.correlationPath, 'John_Smith',Collections.emptyMap())
        and: 'I have a service that depends on another service'
        wireMock.register(matching(nestedRecordingScope.correlationPath +'.*'), get(urlEqualTo("/entry_point")).willReturn(aResponse().proxiedFrom(wireMock.baseUrl()+"/proxied")).atPriority(1))
        wireMock.register(matching(nestedRecordingScope.correlationPath +'.*'), get(urlEqualTo("/proxied/entry_point")).willReturn(aResponse().withBody("hello")).atPriority(1))
        and: 'I have registered a RecordingSpecification to save recordings to the subdirectory "recordings" in the outputRoot'
        def outputRoot = wireMock.getResourceRoot(ScopedAdmin.OUTPUT_RESOURCE_ROOT)
        def requestPattern = new ExtendedRequestPattern(nestedRecordingScope.correlationPath, new RequestPatternBuilder(RequestMethod.GET, urlMatching("/.*")).build())
        def recordingSpecification = new RecordingSpecification()
        recordingSpecification.recordingResponsesTo('recordings')
        def stubMapping = new ExtendedStubMapping(requestPattern, null)
        stubMapping.recordingSpecification=recordingSpecification
        wireMock.register(stubMapping)
        and: 'I invoke this service twice'
        MatcherAssert.assertThat(sendGet("/entry_point", userScope.correlationPath, 0),is(IsEqual.equalTo("hello")))
        MatcherAssert.assertThat(sendGet("/entry_point", userScope.correlationPath, 1),is(IsEqual.equalTo("hello")))


        when:'I stop the parent scope'
        wireMock.stopNestedScope(nestedRecordingScope.correlationPath,Collections.emptyMap())

        then:'I see all root and nested exchanges in the directory'
        outputRoot.clearCache()
        def outputDir = outputRoot.resolveExisting('John_Smith', 'nested1_recording_scope', 'recordings')
        outputDir !=null
        outputDir.list().length == 8
        outputDir.getChild('GET_entry_point_0.txt') != null
        outputDir.getChild('GET_entry_point_0.headers.json') != null
        outputDir.getChild('GET_entry_point_1.txt') != null
        outputDir.getChild('GET_entry_point_1.headers.json') != null
        outputDir.getChild('GET_proxied_entry_point_0.txt') != null
        outputDir.getChild('GET_proxied_entry_point_0.headers.json') != null
        outputDir.getChild('GET_proxied_entry_point_1.txt') != null
        outputDir.getChild('GET_proxied_entry_point_1.headers.json') != null

    }
    def 'Should save all exchanges and nested exchanges in the journal directory specified by a recording mapping in the global scope as per the global journal mode'() {
        given:'I have a nested scope containing a user scope for John Smith'
        def globalCorrelationState = new GlobalCorrelationState('someRun', new URL(wireMock.baseUrl()), new URL(wireMock.baseUrl() + '/sut'), 'sutx')
        globalCorrelationState.globalJournaMode=JournalMode.RECORD
        def globalScope = wireMock.startNewGlobalScope(globalCorrelationState)
        def nestedRecordingScope = wireMock.joinCorrelatedScope(globalScope.correlationPath, 'nested1_recording_scope', Collections.emptyMap())
        def userScope= wireMock.joinUserScope(nestedRecordingScope.correlationPath, 'John_Smith',Collections.emptyMap())
        and: 'I have registered a global RecordingSpecification to save recordings to the subdirectory "journal1" in the journalRoot'
        def journalRoot = wireMock.getResourceRoot(ScopedAdmin.JOURNAL_RESOURCE_ROOT)
        def requestPattern = new ExtendedRequestPattern(globalScope.correlationPath, new RequestPatternBuilder(RequestMethod.GET, urlMatching("/.*")).build())
        def recordingSpecification = new RecordingSpecification()
        recordingSpecification.mapsToJournalDirectory('journal1')
        def stubMapping = new ExtendedStubMapping(requestPattern, null)
        stubMapping.recordingSpecification=recordingSpecification
        wireMock.register(stubMapping)
        and: 'I have a service that depends on another service'
        wireMock.register(matching(nestedRecordingScope.correlationPath +'.*'), get(urlEqualTo("/entry_point")).willReturn(aResponse().proxiedFrom(wireMock.baseUrl()+"/proxied")).atPriority(1))
        wireMock.register(matching(nestedRecordingScope.correlationPath +'.*'), get(urlEqualTo("/proxied/entry_point")).willReturn(aResponse().withBody("hello")).atPriority(1))

        and: 'I invoke this service twice'
        MatcherAssert.assertThat(sendGet("/entry_point", userScope.correlationPath, 0),is(IsEqual.equalTo("hello")))
        MatcherAssert.assertThat(sendGet("/entry_point", userScope.correlationPath, 1),is(IsEqual.equalTo("hello")))


        when:'I stop the global scope'
        wireMock.stopNestedScope(nestedRecordingScope.correlationPath,Collections.emptyMap())
        wireMock.stopGlobalScope(globalScope)

        then:'I see all root and nested exchanges in the directory'
        journalRoot.clearCache()
        def outputDir = journalRoot.resolveExisting('journal1', 'nested1_recording_scope', 'John_Smith')
        outputDir !=null
        outputDir.list().length == 8
        outputDir.getChild('GET_entry_point_0.txt') != null
        outputDir.getChild('GET_entry_point_0.headers.json') != null
        outputDir.getChild('GET_entry_point_1.txt') != null
        outputDir.getChild('GET_entry_point_1.headers.json') != null
        outputDir.getChild('GET_proxied_entry_point_0.txt') != null
        outputDir.getChild('GET_proxied_entry_point_0.headers.json') != null
        outputDir.getChild('GET_proxied_entry_point_1.txt') != null
        outputDir.getChild('GET_proxied_entry_point_1.headers.json') != null

    }
    private String sendGet(String path, String scopePath, int sequenceNumber) throws IOException {
        HttpGet get = new HttpGet("http://localhost:" + wireMock.port() + path)
        get.setHeader(HeaderName.ofTheCorrelationKey(), scopePath)
        get.setHeader(HeaderName.ofTheSequenceNumber(), sequenceNumber + '')
        CloseableHttpResponse response = HttpClientFactory.createClient().execute(get)
        return HttpClientUtils.getEntityAsStringAndCloseStream(response)
    }
}
