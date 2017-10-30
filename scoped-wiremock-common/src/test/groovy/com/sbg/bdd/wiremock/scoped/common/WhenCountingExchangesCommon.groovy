package com.sbg.bdd.wiremock.scoped.common

import com.github.tomakehurst.wiremock.common.HttpClientUtils
import com.github.tomakehurst.wiremock.http.HttpClientFactory
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
    def 'should only count exchanges against the user'() {
        //This is not an overly useful example, but it does test that counting works
        given:'I have a scope containing with a user scope that also contains steps'
        def globalScope = wireMock.startNewGlobalScope(new GlobalCorrelationState('someRun', new URL(wireMock.baseUrl()), new URL(wireMock.baseUrl() + '/sut'), 'sutx'))
        def nestedScope = wireMock.startNestedScope(globalScope.correlationPath , 'nestedScope', Collections.emptyMap())
        //NB!! THe assumption is that actual requests will always be performed on behalf of a user within a Scope
        def userScope= wireMock.startUserScope(nestedScope.correlationPath ,  'user1',Collections.emptyMap())
        and: 'I have one service calling another service'
        wireMock.register(matching(nestedScope.correlationPath +'.*'), get(urlEqualTo("/entry_point")).willReturn(aResponse().proxiedFrom(wireMock.baseUrl()+"/proxied")).atPriority(1))
        wireMock.register(matching(nestedScope.correlationPath +'.*'), get(urlEqualTo("/proxied/entry_point")).willReturn(aResponse().withBody("hello")).atPriority(1))
        and: 'I call it twice within the user scope'
        MatcherAssert.assertThat(sendGet("/entry_point", userScope.correlationPath),is(IsEqual.equalTo("hello")))
        MatcherAssert.assertThat(sendGet("/entry_point", userScope.correlationPath),is(IsEqual.equalTo("hello")))
        and: 'once outside the user scope'
        MatcherAssert.assertThat(sendGet("/entry_point", nestedScope.correlationPath),is(IsEqual.equalTo("hello")))
        when:'I count the exchanges made by the user against the first service'
        def count = wireMock.count(new ExtendedRequestPattern(userScope.correlationPath,get(urlEqualTo("/entry_point")).withHeader(HeaderName.ofTheCorrelationKey(),equalTo(userScope.correlationPath)).build().request))
        then: 'I find two'
        count == 2
    }

    private String sendGet(String path, String scopePath) throws IOException {
        HttpGet get = new HttpGet("http://localhost:" + wireMock.port() + path)
        get.setHeader(HeaderName.ofTheCorrelationKey(), scopePath)
        CloseableHttpResponse response = HttpClientFactory.createClient().execute(get)
        return HttpClientUtils.getEntityAsStringAndCloseStream(response)
    }
}
