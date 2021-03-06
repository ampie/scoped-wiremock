package com.sbg.bdd.wiremock.scoped.common

import com.github.tomakehurst.wiremock.common.HttpClientUtils
import com.github.tomakehurst.wiremock.http.HttpClientFactory
import com.github.tomakehurst.wiremock.http.RequestMethod
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder
import com.sbg.bdd.wiremock.scoped.admin.model.CorrelationState
import com.sbg.bdd.wiremock.scoped.admin.model.ExtendedRequestPattern
import com.sbg.bdd.wiremock.scoped.admin.model.GlobalCorrelationState
import com.sbg.bdd.wiremock.scoped.integration.HeaderName
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpGet
import static org.hamcrest.core.IsEqual.equalTo

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse
import static com.github.tomakehurst.wiremock.client.WireMock.matching
import static com.github.tomakehurst.wiremock.client.WireMock.get
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.core.Is.is

abstract class WhenManagingScopedRecordingsCommon extends ScopedWireMockCommonTest {

    def 'shouldRecordAnExchangeAgainstTheScopeItOccurredIn'(){
        given: 'I have two scopes and I 2 requests in the first and 1 in the second'
        def globalScope = wireMock.startNewGlobalScope(new GlobalCorrelationState('someRun', new URL(wireMock.baseUrl()), new URL(wireMock.baseUrl()+'/sut'),'sutx'))
        def scope1 = wireMock.startNestedScope(globalScope.correlationPath, 'scope1',Collections.emptyMap())
        def scope2 = wireMock.startNestedScope(globalScope.correlationPath, 'scope2',Collections.emptyMap())
        wireMock.register(matching(scope1.correlationPath + '.*'), get(urlEqualTo("/test/uri1")).willReturn(aResponse().withBody("hello1")).atPriority(1))
        wireMock.register(matching(scope2.correlationPath + '.*'), get(urlEqualTo("/test/uri2")).willReturn(aResponse().withBody("hello2")).atPriority(1))
        assertThat(sendGet("/test/uri1", scope1.correlationPath), is(equalTo("hello1")))
        assertThat(sendGet("/test/uri1", scope1.correlationPath), is(equalTo("hello1")))
        assertThat(sendGet("/test/uri2", scope2.correlationPath), is(equalTo("hello2")))

        when:'I extract the matching exchanges in each scope'
        def exchangesAgainstScope1 = wireMock.findMatchingExchanges(matching(scope1.correlationPath), buildRequestPattern("/test/uri1",globalScope));
        def exchangesAgainstScope2 = wireMock.findMatchingExchanges(matching(scope2.correlationPath), buildRequestPattern('/test/uri2', globalScope));

        then:'the first scope reflects 2 exchanges and the second 1 exchange'
        exchangesAgainstScope1.size() == 2
        exchangesAgainstScope2.size() == 1
    }

    private ExtendedRequestPattern buildRequestPattern(String s,CorrelationState scope) {
        return new ExtendedRequestPattern(scope.correlationPath, new RequestPatternBuilder(RequestMethod.GET, urlEqualTo(s)).build())
    }

    def 'shouldDiscardAnExchangeWhenTheScopeItOccurredInStops'(){
        given: 'I have two scopes and I 2 requests in the first and 1 in the second'
        def globalScope = wireMock.startNewGlobalScope(new GlobalCorrelationState('someRun', new URL(wireMock.baseUrl()), new URL(wireMock.baseUrl()+'/sut'),'sutx'))
        def scope1 = wireMock.startNestedScope(globalScope.correlationPath, 'scope1',Collections.emptyMap())
        def scope2 = wireMock.startNestedScope(globalScope.correlationPath, 'scope2',Collections.emptyMap())
        wireMock.register(matching(scope1.correlationPath + '.*'), get(urlEqualTo("/test/uri1")).willReturn(aResponse().withBody("hello1")).atPriority(1))
        wireMock.register(matching(scope2.correlationPath + '.*'), get(urlEqualTo("/test/uri2")).willReturn(aResponse().withBody("hello2")).atPriority(1))
        assertThat(sendGet("/test/uri1", scope1.correlationPath), is(equalTo("hello1")))
        assertThat(sendGet("/test/uri1", scope1.correlationPath), is(equalTo("hello1")))
        assertThat(sendGet("/test/uri2", scope2.correlationPath), is(equalTo("hello2")))

        when:'I stop the first scope'
        wireMock.stopNestedScope(scope1.correlationPath,Collections.emptyMap())
        def exchangesAgainstScope1 = wireMock.findMatchingExchanges(matching(scope1.correlationPath), buildRequestPattern('/test/uri1', globalScope));
        def exchangesAgainstScope2 = wireMock.findMatchingExchanges(matching(scope2.correlationPath), buildRequestPattern('/test/uri2', globalScope));

        then:'the first scope reflects 0 exchanges and the second 1 exchange'
        exchangesAgainstScope1.size() == 0
        exchangesAgainstScope2.size() == 1
    }

    def 'shouldDiscardAnExchangeWhenTheParentOfTheScopeItOccurredInStops'(){
        given: 'I have two scopes and 2 requests in the first and 1 in the second'
        def globalScope = wireMock.startNewGlobalScope(new GlobalCorrelationState('someRun', new URL(wireMock.baseUrl()), new URL(wireMock.baseUrl()+'/sut'),'sutx'))
        def parentScope = wireMock.startNestedScope(globalScope.correlationPath,'nested',Collections.emptyMap())
        def scope1 = wireMock.startNestedScope(parentScope.correlationPath, 'scope1',Collections.emptyMap())
        def scope2 = wireMock.startNestedScope(parentScope.correlationPath, 'scope2',Collections.emptyMap())
        wireMock.register(matching(scope1.correlationPath + '.*'), get(urlEqualTo("/test/uri1")).willReturn(aResponse().withBody("hello1")).atPriority(1))
        wireMock.register(matching(scope2.correlationPath + '.*'), get(urlEqualTo("/test/uri2")).willReturn(aResponse().withBody("hello2")).atPriority(1))
        assertThat(sendGet("/test/uri1", scope1.correlationPath), is(equalTo("hello1")))
        assertThat(sendGet("/test/uri1", scope1.correlationPath), is(equalTo("hello1")))
        assertThat(sendGet("/test/uri2", scope2.correlationPath), is(equalTo("hello2")))

        when:'I stop the parent scope scope'
        wireMock.stopNestedScope(parentScope.correlationPath,Collections.emptyMap())
        def exchangesAgainstScope1 = wireMock.findMatchingExchanges(matching(scope1.correlationPath), buildRequestPattern('/test/uri1', globalScope));
        def exchangesAgainstScope2 = wireMock.findMatchingExchanges(matching(scope2.correlationPath), buildRequestPattern('/test/uri2', globalScope));

        then:'the both scopes reflect 0 exchanges'
        exchangesAgainstScope1.size() == 0
        exchangesAgainstScope2.size() == 0
    }

    def 'shouldDiscardExchangesFromNestedScopeWhenItStops'(){
        given:
        def globalScope = wireMock.startNewGlobalScope(new GlobalCorrelationState('someRun', new URL(wireMock.baseUrl()), new URL(wireMock.baseUrl()+'/sut'),'sutx'))
        def nestedScope = wireMock.startNestedScope(globalScope.correlationPath, 'scope1',Collections.emptyMap())
        wireMock.register(get(urlEqualTo("/test/uri1")).withHeader(HeaderName.ofTheCorrelationKey(), matching(globalScope.correlationPath + '.*')).willReturn(aResponse().withBody("hello1")).atPriority(1));
        assertThat(sendGet("/test/uri1",nestedScope.getCorrelationPath()), is(equalTo("hello1")))
        assertThat(sendGet("/test/uri1", nestedScope.getCorrelationPath()), is(equalTo("hello1")))
        assertThat(sendGet("/test/uri1", globalScope.getCorrelationPath()), is(equalTo("hello1")))
        when:
        wireMock.stopNestedScope(nestedScope.getCorrelationPath(),Collections.emptyMap())
        then:
        def exchangesAgainstNestedScope = wireMock.findMatchingExchanges(matching(nestedScope.correlationPath), buildRequestPattern('/test/uri1', globalScope));
        exchangesAgainstNestedScope.size() == 0
        def exchangesAgainstParentScope = wireMock.findMatchingExchanges(matching(globalScope.correlationPath), buildRequestPattern('/test/uri1', globalScope));
        exchangesAgainstParentScope.size() == 1
    }

    private String sendGet(String path, String scopePath) throws IOException {
        HttpGet get = new HttpGet("http://localhost:" + getWireMockPort() + path)
        get.setHeader(HeaderName.ofTheCorrelationKey(), scopePath)
        CloseableHttpResponse response = HttpClientFactory.createClient().execute(get)
        return HttpClientUtils.getEntityAsStringAndCloseStream(response)
    }



}
