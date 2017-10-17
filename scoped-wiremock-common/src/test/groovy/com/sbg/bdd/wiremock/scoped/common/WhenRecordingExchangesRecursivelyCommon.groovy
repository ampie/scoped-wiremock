package com.sbg.bdd.wiremock.scoped.common

import com.github.tomakehurst.wiremock.common.HttpClientUtils
import com.github.tomakehurst.wiremock.http.HttpClientFactory
import com.github.tomakehurst.wiremock.http.RequestMethod
import com.github.tomakehurst.wiremock.matching.RequestPattern
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

abstract class WhenRecordingExchangesRecursivelyCommon extends  ScopedWireMockCommonTest {
    def 'shouldOnlyRetrieveRootExchangesAgainstStep'() {
        given:'I have a scope containing steps with a nested (user) scope'
        def globalScope = wireMock.startNewGlobalScope(new GlobalCorrelationState('someRun', new URL(wireMock.baseUrl()), new URL(wireMock.baseUrl() + '/sut'), 'sutx'))
        def stepContainingScope = wireMock.joinCorrelatedScope(globalScope.correlationPath +'/' + 'stepContainingScope', Collections.emptyMap())
        //NB!! THe assumption is that actual requests will always be performed on behalf of a user within a Scope
        def userScope= wireMock.joinCorrelatedScope(stepContainingScope.correlationPath + '/user1',Collections.emptyMap())
        wireMock.startStep(stepContainingScope.getCorrelationPath(), 'step1',Collections.emptyMap())
        wireMock.register(matching(stepContainingScope.correlationPath +'.*'), get(urlEqualTo("/entry_point")).willReturn(aResponse().proxiedFrom(wireMock.baseUrl()+"/proxied")).atPriority(1))
        wireMock.register(matching(stepContainingScope.correlationPath +'.*'), get(urlEqualTo("/proxied/entry_point")).willReturn(aResponse().withBody("hello")).atPriority(1))
        assertThat(sendGet("/entry_point", userScope.correlationPath),is(IsEqual.equalTo("hello")))
        wireMock.stopStep(stepContainingScope.correlationPath, "step1",Collections.emptyMap())
        assertThat(sendGet("/entry_point", userScope.correlationPath),is(IsEqual.equalTo("hello")))
        when:
        def exchangesAgainstStep = wireMock.findExchangesAgainstStep(stepContainingScope.getCorrelationPath(), "step1")
        def requestPattern = new ExtendedRequestPattern(stepContainingScope.correlationPath, new RequestPatternBuilder(RequestMethod.GET, urlMatching("/.*")).build())
        def exchangesAgainstScope = wireMock.findMatchingExchanges(matching(stepContainingScope.getCorrelationPath() +'.*'), requestPattern)
        then:
        exchangesAgainstScope.size() == 4
        exchangesAgainstStep.size() == 1
        exchangesAgainstStep.get(0).getNestedExchanges().size() == 1
    }

    private String sendGet(String path, String scopePath) throws IOException {
        HttpGet get = new HttpGet("http://localhost:" + wireMock.port() + path)
        get.setHeader(HeaderName.ofTheCorrelationKey(), scopePath)
        CloseableHttpResponse response = HttpClientFactory.createClient().execute(get)
        return HttpClientUtils.getEntityAsStringAndCloseStream(response)
    }
}
