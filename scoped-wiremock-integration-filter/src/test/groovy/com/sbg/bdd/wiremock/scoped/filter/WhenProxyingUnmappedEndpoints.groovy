package com.sbg.bdd.wiremock.scoped.filter

import com.sbg.bdd.wiremock.scoped.integration.BaseDependencyInjectorAdaptor
import com.sbg.bdd.wiremock.scoped.integration.BaseWireMockCorrelationState
import com.sbg.bdd.wiremock.scoped.integration.DependencyInjectionAdaptorFactory
import com.sbg.bdd.wiremock.scoped.integration.EndPointRegistry
import com.sbg.bdd.wiremock.scoped.integration.HeaderName
import spock.lang.Specification

import javax.servlet.FilterChain
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class WhenProxyingUnmappedEndpoints extends Specification{
    def 'it should proxy all known endpoints when switched on'(){
        DependencyInjectionAdaptorFactory.useAdapter(new BaseDependencyInjectorAdaptor())
        given:
        System.setProperty(InboundCorrelationPathFilter.SCOPED_WIREMOCK_ENABLED, "true")
        def commands = new ArrayList()
        HttpCommandExecutor.INSTANCE=Mock(HttpCommandExecutor){
            execute(_) >> {args ->
                commands << args[0]
            }
        }
        BaseDependencyInjectorAdaptor.CURRENT_CORRELATION_STATE=new BaseWireMockCorrelationState()
        KnownEndpointRegistry.getInstance().registerRestEndpoint('endpoint1')
        KnownEndpointRegistry.getInstance().registerSoapEndpoint('endpoint2')
        KnownEndpointRegistry.getInstance().registerTransitiveRestEndpoint('endpoint3', 'http://host3.com')
        KnownEndpointRegistry.getInstance().registerTransitiveSoapEndpoint('endpoint4', 'http://host4.com')
        BaseDependencyInjectorAdaptor.ENDPOINT_REGISTRY=Mock(EndPointRegistry){
            endpointUrlFor('endpoint1') >> new URL('http://host1.com')
            endpointUrlFor('endpoint2') >> new URL('http://host2.com')
        }
        def filter = new InboundCorrelationPathFilter()
        filter.init(null)
        def request = Mock(HttpServletRequest){
            getRequestURI() >> '/somewhere'
            getHeader(HeaderName.ofTheCorrelationKey()) >> 'localhost/8080/scopepath'
            getHeader(HeaderName.toProxyUnmappedEndpoints()) >> 'true'
            getHeaders(HeaderName.ofTheServiceInvocationCount()) >> new Vector().elements()
        }
        def response = Mock(HttpServletResponse){
        }

        when:
        filter.doFilter(request,response,Mock(FilterChain))

        then:
        commands.size() == 4


    }
}
