package com.sbg.bdd.wiremock.scoped.resteasy

import com.sbg.bdd.wiremock.scoped.integration.BaseDependencyInjectorAdaptor
import com.sbg.bdd.wiremock.scoped.integration.BaseWireMockCorrelationState
import com.sbg.bdd.wiremock.scoped.integration.DependencyInjectionAdaptorFactory
import com.sbg.bdd.wiremock.scoped.integration.HeaderName
import com.sbg.bdd.wiremock.scoped.integration.ServiceInvocationCount
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
        state.set('localhost/8080/somepath',1,true)
        state.initSequenceNumberFor(new ServiceInvocationCount('1|endpoint1|6'))
        state.initSequenceNumberFor(new ServiceInvocationCount('1|endpoint2|8'))
        def interceptor = new OutboundCorrelationPathRestInterceptor()

        def request = new ClientRequest('http://localhost:8080/base').header(HeaderName.ofTheOriginalUrl(), new URL('http://somewhere.com/'))
        def response = new BaseClientResponse(null);
        def ctx = new ClientExecutionContextImpl(null,null,request){
            @Override
            ClientResponse proceed() throws Exception {
                return response
            }
        }

        when:
        interceptor.execute(ctx)


        def headers = request.getHeadersAsObjects()
        then:
        headers.get(HeaderName.ofTheCorrelationKey())[0] == 'localhost/8080/somepath'
        headers.get(HeaderName.ofTheOriginalUrl())[0] == 'http://somewhere.com/base'
        headers.get(HeaderName.ofTheSequenceNumber())[0] == '1'
        headers.get(HeaderName.toProxyUnmappedEndpoints())[0] == 'true'
        headers.get(HeaderName.ofTheServiceInvocationCount())[0] == '1|endpoint1|6'
        headers.get(HeaderName.ofTheServiceInvocationCount())[1] == '1|endpoint2|8'
        headers.get(HeaderName.ofTheServiceInvocationCount())[2] == '1|http:null://somewhere.com/base|1'//null because we can't fake a method with sending a request
    }
    def 'it should extract all the correct incoming headers'(){
        given:
        DependencyInjectionAdaptorFactory.useAdapter(new BaseDependencyInjectorAdaptor())
        def state= BaseDependencyInjectorAdaptor.CURRENT_CORRELATION_STATE=new BaseWireMockCorrelationState()
        state.set('localhost/8080/somepath',1,true)
        state.initSequenceNumberFor(new ServiceInvocationCount('1|endpoint1|6'))
        state.initSequenceNumberFor(new ServiceInvocationCount('1|endpoint2|8'))
        def interceptor = new OutboundCorrelationPathRestInterceptor()

        def request = new ClientRequest('http://somewhere.com/').header(HeaderName.ofTheOriginalUrl(), new URL('http://somewhere.com/'));
        def response = new BaseClientResponse(null);
        response.setHeaders(new MultivaluedMapImpl<String, String>());
        response.getHeaders().add(HeaderName.ofTheCorrelationKey(),'localhost/8080/somepath')
        response.getHeaders().add(HeaderName.ofTheServiceInvocationCount(),'1|endpoint1|9')
        response.getHeaders().add(HeaderName.ofTheServiceInvocationCount(),'1|endpoint2|16')
        def ctx = new ClientExecutionContextImpl(null,null,request){
            @Override
            ClientResponse proceed() throws Exception {
                return response
            }
        }

        when:
        interceptor.execute(ctx)

        then:
        state.getNextSequenceNumberFor('endpoint1') == 10
        state.getNextSequenceNumberFor('endpoint2') == 17
    }
}
