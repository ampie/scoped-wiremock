package com.sbg.bdd.wiremock.scoped.server

import com.github.tomakehurst.wiremock.common.FileSource
import com.github.tomakehurst.wiremock.extension.Parameters
import com.github.tomakehurst.wiremock.http.HttpHeader
import com.github.tomakehurst.wiremock.http.HttpHeaders
import com.github.tomakehurst.wiremock.http.Request
import com.github.tomakehurst.wiremock.http.Response
import com.sbg.bdd.wiremock.scoped.admin.model.CorrelationState
import com.sbg.bdd.wiremock.scoped.admin.model.GlobalCorrelationState
import com.sbg.bdd.wiremock.scoped.admin.model.ServiceInvocationCount
import com.sbg.bdd.wiremock.scoped.integration.HeaderName
import com.sbg.bdd.wiremock.scoped.integration.RuntimeCorrelationState
import com.sbg.bdd.wiremock.scoped.server.junit.WireMockRuleConfiguration
import spock.lang.Specification

class WhenReceivingRequestsDecorator extends Specification {
    ScopedWireMockServer server
    def setup(){
        this.server = new ScopedWireMockServer(WireMockRuleConfiguration.DYNAMIC_PORT)
        server.start()
    }
    def cleanup(){
        try {
            server.shutdown()
        } catch (e) {
        }
    }

    def 'update the serviceInvocationCount on the current CorrelationState if received from a downstream response'() {
        given:'an external service returns serviceInvocationCount header from an external service on its response'
        CorrelationState correlationState =server.startNewGlobalScope(new GlobalCorrelationState('testRunX',new URL(server.baseUrl()), new URL(server.baseUrl() +'/sut'),'sutx'))
        correlationState.serviceInvocationCounts.add(new ServiceInvocationCount('1|http://test.com:8080/this?queryPartm=123|3'))
        server.syncCorrelatedScope(correlationState)
        HttpHeaders headers = new HttpHeaders()
                .plus(new HttpHeader(HeaderName.ofTheCorrelationKey(), correlationState.getCorrelationPath()))
                .plus(new HttpHeader(HeaderName.ofTheServiceInvocationCount(), "1|http://test.com:8080/this?queryPartm=123|7"))

        Response response = Mock(Response.class){
            getHeaders() >> headers
        }

        when:'ScopedWireMock receives that response'
        new ScopeUpdatingResponseTransformer().transform(Mock(Request.class), response, Mock(FileSource.class), new Parameters())

        then:'the CorrelationState within which the initial request was made must reflect the serviceInvocationCount received'
        if(RuntimeCorrelationState.ON) {
            Integer actualCount = server.getCorrelatedScope(correlationState.getCorrelationPath()).getServiceInvocationCount(ServiceInvocationCount.keyOf(1, "http://test.com:8080/this?queryPartm=123")).count
            actualCount == 7
        }
    }

    def 'update the serviceInvocationCount on the current CorrelationState from an upstream request only if none was received from the response'() {
        given:
        CorrelationState correlationState =server.startNewGlobalScope(new GlobalCorrelationState('testRunX',new URL(server.baseUrl()), new URL(server.baseUrl() +'/sut'),'sutx'))
        correlationState.serviceInvocationCounts.add(new ServiceInvocationCount('1|http://test.com:8080/this?queryPartm=123|3'))

        server.syncCorrelatedScope(correlationState)
        HttpHeaders headers = new HttpHeaders()
                .plus(new HttpHeader(HeaderName.ofTheCorrelationKey(), correlationState.getCorrelationPath()))
                .plus(new HttpHeader(HeaderName.ofTheServiceInvocationCount(), "1|http://test.com:8080/this?queryPartm=123|6"))

        def request = Mock(Request.class){
            getHeaders() >> headers
        }
        def response = Mock(Response.class){
            getHeaders() >> new HttpHeaders()
        }

        when:
        new ScopeUpdatingResponseTransformer().transform(request, response, Mock(FileSource.class), new Parameters())

        then:
        if(RuntimeCorrelationState.ON) {
            Integer actualCount = server.getCorrelatedScope(correlationState.getCorrelationPath()).getServiceInvocationCount(ServiceInvocationCount.keyOf(1, "http://test.com:8080/this?queryPartm=123")).count
            actualCount == 6
        }
    }

}
