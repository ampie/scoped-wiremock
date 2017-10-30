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
import static com.github.tomakehurst.wiremock.client.WireMock.*
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.core.Is.is

abstract class WhenRetrievingExchangesCommon extends  ScopedWireMockCommonTest {
    def 'Should retrieve root exchanges against a step, not nested exchanges'() {
        given:'I have a scope containing steps with a user scope'
        def globalScope = wireMock.startNewGlobalScope(new GlobalCorrelationState('someRun', new URL(wireMock.baseUrl()), new URL(wireMock.baseUrl() + '/sut'), 'sutx'))
        def stepContainingScope = wireMock.startNestedScope(globalScope.correlationPath, 'stepContainingScope', Collections.emptyMap())
        //NB!! THe assumption is that actual requests will always be performed on behalf of a user within a Scope
        def userScope= wireMock.startUserScope(stepContainingScope.correlationPath, 'user1',Collections.emptyMap())
        wireMock.startStep(stepContainingScope.getCorrelationPath(), 'step1',Collections.emptyMap())
        and: 'I have a service that depends on another service'
        wireMock.register(matching(stepContainingScope.correlationPath +'.*'), get(urlEqualTo("/entry_point")).willReturn(aResponse().proxiedFrom(wireMock.baseUrl()+"/proxied")).atPriority(1))
        wireMock.register(matching(stepContainingScope.correlationPath +'.*'), get(urlEqualTo("/proxied/entry_point")).willReturn(aResponse().withBody("hello")).atPriority(1))
        and: 'I invoke the service that in turn invokes the other service within the scope of a step'
        assertThat(sendGet("/entry_point", userScope.correlationPath),is(IsEqual.equalTo("hello")))
        when:'I retrieve the exchanges against the step'
        def exchangesAgainstStep = wireMock.findExchangesAgainstStep(stepContainingScope.getCorrelationPath(), "step1")
        then:'I only see the a single top level exchange'
        exchangesAgainstStep.size() == 1
        exchangesAgainstStep.get(0).getNestedExchanges().size() == 1
    }
    def 'Should retrieve all root exchanges and nested exchanges flattend to a list against the responsible user scope'() {
        given:'I have a scope containing steps with a nested (user) scope'
        def globalScope = wireMock.startNewGlobalScope(new GlobalCorrelationState('someRun', new URL(wireMock.baseUrl()), new URL(wireMock.baseUrl() + '/sut'), 'sutx'))
        def stepContainingScope = wireMock.startNestedScope(globalScope.correlationPath, 'stepContainingScope', Collections.emptyMap())
        //NB!! THe assumption is that actual requests will always be performed on behalf of a user within a Scope
        def userScope= wireMock.startUserScope(stepContainingScope.correlationPath, 'user1',Collections.emptyMap())
        wireMock.startStep(stepContainingScope.getCorrelationPath(), 'step1',Collections.emptyMap())
        and: 'I have a service that depends on another service'
        wireMock.register(matching(stepContainingScope.correlationPath +'.*'), get(urlEqualTo("/entry_point")).willReturn(aResponse().proxiedFrom(wireMock.baseUrl()+"/proxied")).atPriority(1))
        wireMock.register(matching(stepContainingScope.correlationPath +'.*'), get(urlEqualTo("/proxied/entry_point")).willReturn(aResponse().withBody("hello")).atPriority(1))
        and: 'I invoke the service that in turn invokes the other service within the scope of a step'
        assertThat(sendGet("/entry_point", userScope.correlationPath),is(IsEqual.equalTo("hello")))
        and: 'I invoke the service that in turn invokes the other service outside the scope of a step'
        wireMock.stopStep(stepContainingScope.correlationPath, "step1",Collections.emptyMap())
        assertThat(sendGet("/entry_point", userScope.correlationPath),is(IsEqual.equalTo("hello")))
        when:'I retrieve the exchanges against the user scope'
        def requestPattern = new ExtendedRequestPattern(stepContainingScope.correlationPath, new RequestPatternBuilder(RequestMethod.GET, urlMatching("/.*")).build())
        def exchangesAgainstScope = wireMock.findMatchingExchanges(matching(userScope.getCorrelationPath() ), requestPattern)
        then:'I see all root and nested exchanges against the user scope'
        exchangesAgainstScope.size() == 4
        exchangesAgainstScope.get(0).getNestedExchanges().size() == 0
        exchangesAgainstScope.get(1).getNestedExchanges().size() == 0
        exchangesAgainstScope.get(2).getNestedExchanges().size() == 0
        exchangesAgainstScope.get(3).getNestedExchanges().size() == 0
    }
    private String sendGet(String path, String scopePath) throws IOException {
        HttpGet get = new HttpGet("http://localhost:" + wireMock.port() + path)
        get.setHeader(HeaderName.ofTheCorrelationKey(), scopePath)
        CloseableHttpResponse response = HttpClientFactory.createClient().execute(get)
        return HttpClientUtils.getEntityAsStringAndCloseStream(response)
    }
}
