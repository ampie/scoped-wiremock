package com.sbg.bdd.wiremock.scoped.resteasy

import com.sbg.bdd.wiremock.scoped.integration.BaseDependencyInjectorAdaptor
import com.sbg.bdd.wiremock.scoped.integration.BaseWireMockCorrelationState
import com.sbg.bdd.wiremock.scoped.integration.DependencyInjectionAdaptorFactory
import com.sbg.bdd.wiremock.scoped.integration.HeaderName
import org.jboss.resteasy.client.ClientRequest
import org.jboss.resteasy.client.ClientResponse
import org.jboss.resteasy.client.core.BaseClientResponse
import org.jboss.resteasy.core.interception.ClientExecutionContextImpl
import org.jboss.resteasy.specimpl.MultivaluedMapImpl
import spock.lang.Specification

class WhenSendingRequestsToDownstreamRestServices extends Specification{
    def 'it should serialize all the correct outgoing headers'(){
        given:
        DependencyInjectionAdaptorFactory.useAdapter(new BaseDependencyInjectorAdaptor())
        def state= BaseDependencyInjectorAdaptor.CURRENT_CORRELATION_STATE=new BaseWireMockCorrelationState()
        state.set('localhost/8080/somepath',true)
        state.initSequenceNumberFor('endpoint1',6)
        state.initSequenceNumberFor('endpoint2',8)
        def interceptor = new OutboundCorrelationPathRestInterceptor()

        def request = new ClientRequest('http://somewhere.com/');
        def response = new BaseClientResponse(null);
        def ctx = new ClientExecutionContextImpl(null,null,request){
            @Override
            ClientResponse proceed() throws Exception {
                return response
            }
        }

        when:
        interceptor.execute(ctx)

        then:
        request.getHeadersAsObjects().get(HeaderName.ofTheCorrelationKey())[0] == 'localhost/8080/somepath'
        request.getHeadersAsObjects().get(HeaderName.ofTheSequenceNumber())[0] == '1'
        request.getHeadersAsObjects().get(HeaderName.toProxyUnmappedEndpoints())[0] == 'true'
        request.getHeadersAsObjects().get(HeaderName.ofTheServiceInvocationCount())[0] == 'endpoint1|6'
        request.getHeadersAsObjects().get(HeaderName.ofTheServiceInvocationCount())[1] == 'endpoint2|8'
    }
    def 'it should extract all the correct incoming headers'(){
        given:
        DependencyInjectionAdaptorFactory.useAdapter(new BaseDependencyInjectorAdaptor())
        def state= BaseDependencyInjectorAdaptor.CURRENT_CORRELATION_STATE=new BaseWireMockCorrelationState()
        state.set('localhost/8080/somepath',true)
        state.initSequenceNumberFor('endpoint1',6)
        state.initSequenceNumberFor('endpoint2',8)
        def interceptor = new OutboundCorrelationPathRestInterceptor()

        def request = new ClientRequest('http://somewhere.com/');
        def response = new BaseClientResponse(null);
        response.setHeaders(new MultivaluedMapImpl<String, String>());
        response.getHeaders().add(HeaderName.ofTheCorrelationKey(),'localhost/8080/somepath')
        response.getHeaders().add(HeaderName.ofTheServiceInvocationCount(),'endpoint1|9')
        response.getHeaders().add(HeaderName.ofTheServiceInvocationCount(),'endpoint2|16')
        def ctx = new ClientExecutionContextImpl(null,null,request){
            @Override
            ClientResponse proceed() throws Exception {
                return response
            }
        }

        when:
        interceptor.execute(ctx)

        then:
        state.getSequenceNumbers().get('endpoint1') == 9
        state.getSequenceNumbers().get('endpoint2') == 16
    }
}
