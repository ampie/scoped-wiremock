package com.sbg.bdd.wiremock.scoped.jaxrs

import com.sbg.bdd.wiremock.scoped.integration.BaseDependencyInjectorAdaptor
import com.sbg.bdd.wiremock.scoped.integration.BaseRuntimeCorrelationState
import com.sbg.bdd.wiremock.scoped.integration.DependencyInjectionAdaptorFactory
import com.sbg.bdd.wiremock.scoped.integration.HeaderName
import com.sbg.bdd.wiremock.scoped.integration.ServiceInvocationCount
import spock.lang.Specification

import javax.ws.rs.client.ClientRequestContext
import javax.ws.rs.client.ClientResponseContext
import javax.ws.rs.core.MultivaluedHashMap

class WhenSendingRequestsToDownstreamRestServices extends Specification{
    def 'it should serialize all the correct outgoing headers'(){
        given:
        DependencyInjectionAdaptorFactory.useAdaptor(new BaseDependencyInjectorAdaptor())
        def state= BaseDependencyInjectorAdaptor.CURRENT_CORRELATION_STATE=new BaseRuntimeCorrelationState()
        state.set('localhost/8080/somepath',101, true)
        state.initSequenceNumberFor(new ServiceInvocationCount('101|endpoint1|6'))
        state.initSequenceNumberFor(new ServiceInvocationCount('101|endpoint2|8'))
        def filter = new OutboundRequestCorrelationKeyFilter()
        def headers  = new MultivaluedHashMap()
        headers.add(HeaderName.ofTheOriginalUrl(), 'http://somewhere.com/')
        def request = Mock(ClientRequestContext){
            getHeaders() >> headers
            getUri() >> URI.create("http://localhost:8080/base")
            getMethod() >> 'GET'
        }
        when:
        filter.filter(request)


        then:
        headers.get(HeaderName.ofTheCorrelationKey())[0] == 'localhost/8080/somepath'
        headers.get(HeaderName.ofTheOriginalUrl())[0] == 'http://somewhere.com/base'
        headers.get(HeaderName.ofTheSequenceNumber())[0] == '1'
        headers.get(HeaderName.toProxyUnmappedEndpoints())[0] == 'true'
        headers.get(HeaderName.ofTheServiceInvocationCount())[0] == '101|endpoint1|6'
        headers.get(HeaderName.ofTheServiceInvocationCount())[1] == '101|endpoint2|8'
        headers.get(HeaderName.ofTheServiceInvocationCount())[2] == '101|http:GET://somewhere.com/base|1'
    }
    def 'it should extract all the correct incoming headers'(){
        given:
        DependencyInjectionAdaptorFactory.useAdaptor(new BaseDependencyInjectorAdaptor())
        def state= BaseDependencyInjectorAdaptor.CURRENT_CORRELATION_STATE=new BaseRuntimeCorrelationState()
        state.set('localhost/8080/somepath',1, true)
        state.initSequenceNumberFor(new ServiceInvocationCount('1|endpoint1|6'))
        state.initSequenceNumberFor(new ServiceInvocationCount('1|endpoint2|8'))
        def filter  = new InboundResponseCorrelationKeyFilter()

        def request = Mock(ClientRequestContext)
        def headers  = new MultivaluedHashMap()
        def response = Mock(ClientResponseContext){
            getHeaders() >> headers
        }

        response.getHeaders().add(HeaderName.ofTheCorrelationKey(),'localhost/8080/somepath')
        response.getHeaders().add(HeaderName.ofTheServiceInvocationCount(),'1|endpoint1|9')
        response.getHeaders().add(HeaderName.ofTheServiceInvocationCount(),'1|endpoint2|16')

        when:
        filter.filter(request,response)

        then:
        state.getNextSequenceNumberFor('endpoint1') == 10
        state.getNextSequenceNumberFor('endpoint2') == 17
    }
}
