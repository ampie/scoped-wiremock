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
import org.hamcrest.MatcherAssert
import org.hamcrest.core.IsEqual

import static com.github.tomakehurst.wiremock.client.WireMock.*
import static org.hamcrest.core.Is.is

abstract class WhenCountingExchangesCommon extends  ScopedWireMockCommonTest {
    def 'should only count top level exchanges'() {
        given:'I have a scope containing steps with a nested (user) scope'
        def globalScope = wireMock.startNewGlobalScope(new GlobalCorrelationState('someRun', new URL(wireMock.baseUrl()), new URL(wireMock.baseUrl() + '/sut'), 'sutx'))
        def nestedScope = wireMock.joinCorrelatedScope(globalScope.correlationPath +'/' + 'nestedScope', Collections.emptyMap())
        //NB!! THe assumption is that actual requests will always be performed on behalf of a user within a Scope
        def userScope= wireMock.joinCorrelatedScope(nestedScope.correlationPath + '/user1',Collections.emptyMap())
        wireMock.register(matching(nestedScope.correlationPath +'.*'), get(urlEqualTo("/entry_point")).willReturn(aResponse().proxiedFrom(wireMock.baseUrl()+"/proxied")).atPriority(1))
        wireMock.register(matching(nestedScope.correlationPath +'.*'), get(urlEqualTo("/proxied/entry_point")).willReturn(aResponse().withBody("hello")).atPriority(1))
        MatcherAssert.assertThat(sendGet("/entry_point", userScope.correlationPath),is(IsEqual.equalTo("hello")))
        MatcherAssert.assertThat(sendGet("/entry_point", userScope.correlationPath),is(IsEqual.equalTo("hello")))
        when:
        def count = wireMock.count(new ExtendedRequestPattern(userScope.correlationPath,get(urlEqualTo("/entry_point")).build().request))
        then:
        count == 2
    }

    private String sendGet(String path, String scopePath) throws IOException {
        HttpGet get = new HttpGet("http://localhost:" + wireMock.port() + path)
        get.setHeader(HeaderName.ofTheCorrelationKey(), scopePath)
        CloseableHttpResponse response = HttpClientFactory.createClient().execute(get)
        return HttpClientUtils.getEntityAsStringAndCloseStream(response)
    }
}
