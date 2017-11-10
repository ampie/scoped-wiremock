package com.sbg.bdd.wiremock.scoped.spring

import com.sbg.bdd.wiremock.scoped.integration.BaseDependencyInjectorAdaptor
import com.sbg.bdd.wiremock.scoped.integration.BaseRuntimeCorrelationState
import com.sbg.bdd.wiremock.scoped.integration.DependencyInjectionAdaptorFactory
import com.sbg.bdd.wiremock.scoped.integration.HeaderName
import com.sbg.bdd.wiremock.scoped.integration.RuntimeCorrelationState
import com.sbg.bdd.wiremock.scoped.integration.ServiceInvocationCount
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpResponse
import spock.lang.Specification

class WhenSendingRequestsToDownstreamRestServices extends Specification {
    def 'it should serialize all the correct outgoing headers'() {
        given:
        DependencyInjectionAdaptorFactory.useAdaptor(new BaseDependencyInjectorAdaptor())
        def state = BaseDependencyInjectorAdaptor.CURRENT_CORRELATION_STATE = new BaseRuntimeCorrelationState()
        state.set('localhost/8080/somepath', 1, true)
        state.initSequenceNumberFor(new ServiceInvocationCount('1|endpoint1|6'))
        state.initSequenceNumberFor(new ServiceInvocationCount('1|endpoint2|8'))
        def interceptor = new OutboundCorrelationKeyInterceptor()
        def headers = new HttpHeaders()
        headers.add(HeaderName.ofTheOriginalUrl(), 'http://somehost.com')
        def request = Mock(HttpRequest) {
            getHeaders() >> headers
            getURI() >> URI.create("http://localhost:8080/base")
            getMethod() >> HttpMethod.GET
        }
        def response = Mock(ClientHttpResponse) {
            getHeaders() >> new HttpHeaders()
        }
        def ctx = Mock(ClientHttpRequestExecution) {
            execute(_, _) >> response
        }

        when:
        interceptor.intercept(request, null, ctx)

        then:
        request.getHeaders().get(HeaderName.ofTheCorrelationKey())[0] == 'localhost/8080/somepath'
        request.getHeaders().get(HeaderName.ofTheOriginalUrl())[0] == 'http://somehost.com/base'
        request.getHeaders().get(HeaderName.toProxyUnmappedEndpoints())[0] == 'true'
        request.getHeaders().get(HeaderName.ofTheThreadContextId())[0] == '1'
        if (RuntimeCorrelationState.ON) {
            request.getHeaders().get(HeaderName.ofTheSequenceNumber())[0] == '1'
            request.getHeaders().get(HeaderName.ofTheServiceInvocationCount())[0] == '1|endpoint1|6'
            request.getHeaders().get(HeaderName.ofTheServiceInvocationCount())[1] == '1|endpoint2|8'
            request.getHeaders().get(HeaderName.ofTheServiceInvocationCount())[2] == '1|http:GET://somehost.com/base|1'
        }
    }

    def 'it should extract all the correct incoming headers'() {
        given:
        DependencyInjectionAdaptorFactory.useAdaptor(new BaseDependencyInjectorAdaptor())
        def state = BaseDependencyInjectorAdaptor.CURRENT_CORRELATION_STATE = new BaseRuntimeCorrelationState()
        state.set('localhost/8080/somepath', 1, true)
        state.initSequenceNumberFor(new ServiceInvocationCount('1|endpoint1|6'))
        state.initSequenceNumberFor(new ServiceInvocationCount('1|endpoint2|8'))
        def interceptor = new OutboundCorrelationKeyInterceptor()

        def request = Mock(HttpRequest) {
            getHeaders() >> new HttpHeaders()
            getURI() >> URI.create("http://somehost.com/")
            getMethod() >> HttpMethod.GET
        }
        def headers = new HttpHeaders()
        def response = Mock(ClientHttpResponse) {
            getHeaders() >> headers
        }
        response.getHeaders().add(HeaderName.ofTheCorrelationKey(), 'localhost/8080/somepath')
        response.getHeaders().add(HeaderName.ofTheServiceInvocationCount(), '1|endpoint1|9')
        response.getHeaders().add(HeaderName.ofTheServiceInvocationCount(), '1|endpoint2|16')
        def ctx = Mock(ClientHttpRequestExecution) {
            execute(_, _) >> response
        }


        when:
        interceptor.intercept(request, null, ctx)

        then:
        state.getNextSequenceNumberFor('endpoint1') == 10
        state.getNextSequenceNumberFor('endpoint2') == 17
    }
}
