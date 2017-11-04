package com.sbg.bdd.wiremock.scoped.common

import com.github.tomakehurst.wiremock.common.HttpClientUtils
import com.github.tomakehurst.wiremock.http.HttpClientFactory
import com.github.tomakehurst.wiremock.http.RequestMethod
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder
import com.sbg.bdd.wiremock.scoped.admin.model.ExtendedRequestPattern
import com.sbg.bdd.wiremock.scoped.admin.model.GlobalCorrelationState
import com.sbg.bdd.wiremock.scoped.integration.HeaderName
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpGet
import org.hamcrest.core.IsEqual

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse
import static com.github.tomakehurst.wiremock.client.WireMock.get
import static com.github.tomakehurst.wiremock.client.WireMock.matching
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.core.Is.is

abstract class WhenRecordingResponsesCommon extends ScopedWireMockCommonTest{
    def 'Should record all exchanges and nested exchanges in a specified directory'() {
        given:'I have a scope containing steps with a nested (user) scope'
        def globalScope = wireMock.startNewGlobalScope(new GlobalCorrelationState('someRun', new URL(wireMock.baseUrl()), new URL(wireMock.baseUrl() + '/sut'), 'sutx'))
        def stepContainingScope = wireMock.startNestedScope(globalScope.correlationPath, 'stepContainingScope', Collections.emptyMap())
        def userScope= wireMock.startUserScope(stepContainingScope.correlationPath, 'user1',Collections.emptyMap())
        wireMock.startStep(stepContainingScope.getCorrelationPath(), 'step1',Collections.emptyMap())
        and: 'I have a service that depends on another service'
        wireMock.register(matching(stepContainingScope.correlationPath +'.*'), get(urlEqualTo("/entry_point")).willReturn(aResponse().proxiedFrom(wireMock.baseUrl()+"/proxied")).atPriority(1))
        wireMock.register(matching(stepContainingScope.correlationPath +'.*'), get(urlEqualTo("/proxied/entry_point")).willReturn(aResponse().withBody("hello")).atPriority(1))
        and: 'I invoke this service twice'
        assertThat(sendGet("/entry_point", userScope.correlationPath, 0),is(IsEqual.equalTo("hello")))
        assertThat(sendGet("/entry_point", userScope.correlationPath, 1),is(IsEqual.equalTo("hello")))
        and: 'I have a directory'
        def outputRoot = wireMock.getResourceRoot('outputRoot')

        when:'I record the exchanges against the user scope'
        def requestPattern = new ExtendedRequestPattern(stepContainingScope.correlationPath, new RequestPatternBuilder(RequestMethod.GET, urlMatching("/.*")).build())
        wireMock.saveRecordingsForRequestPattern(matching(userScope.getCorrelationPath() ), requestPattern,outputRoot)

        then:'I see all root and nested exchanges in the directory'
        outputRoot.clearCache()
        outputRoot.list().length==16
        outputRoot.getChild('GET_entry_point_1_0.txt') != null
        outputRoot.getChild('GET_entry_point_1_0.headers.json') != null
        outputRoot.getChild('GET_entry_point_1_0.request_body.txt') != null
        outputRoot.getChild('GET_entry_point_1_0.request_headers.json') != null
        outputRoot.getChild('GET_entry_point_1_1.txt') != null
        outputRoot.getChild('GET_entry_point_1_1.headers.json') != null
        outputRoot.getChild('GET_proxied_entry_point_1_0.txt') != null
        outputRoot.getChild('GET_proxied_entry_point_1_0.headers.json') != null
        outputRoot.getChild('GET_proxied_entry_point_1_1.txt') != null
        outputRoot.getChild('GET_proxied_entry_point_1_1.headers.json') != null

    }
    private String sendGet(String path, String scopePath, int sequenceNumber) throws IOException {
        HttpGet get = new HttpGet("http://localhost:" + wireMock.port() + path)
        get.setHeader(HeaderName.ofTheCorrelationKey(), scopePath)
        get.setHeader(HeaderName.ofTheSequenceNumber(), sequenceNumber + '')
        get.setHeader(HeaderName.ofTheThreadContextId(),  '1')
        CloseableHttpResponse response = HttpClientFactory.createClient().execute(get)
        return HttpClientUtils.getEntityAsStringAndCloseStream(response)
    }
}
