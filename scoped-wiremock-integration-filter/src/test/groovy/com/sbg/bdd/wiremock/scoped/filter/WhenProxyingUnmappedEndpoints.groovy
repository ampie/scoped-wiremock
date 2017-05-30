package com.sbg.bdd.wiremock.scoped.filter

import com.sbg.bdd.wiremock.scoped.integration.BaseDependencyInjectorAdaptor
import com.sbg.bdd.wiremock.scoped.integration.BaseWireMockCorrelationState
import com.sbg.bdd.wiremock.scoped.integration.DependencyInjectionAdaptorFactory
import com.sbg.bdd.wiremock.scoped.integration.EndPointRegistry
import com.sbg.bdd.wiremock.scoped.integration.HeaderName
import com.sbg.bdd.wiremock.scoped.integration.PropertiesEndpointRegistry
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
        EndpointTypeTracker.getInstance().registerRestEndpoint('endpoint1')
        EndpointTypeTracker.getInstance().registerSoapEndpoint('endpoint2')
        EndpointTypeTracker.getInstance().registerAdditionalRestEndpoint('endpoint3')
        EndpointTypeTracker.getInstance().registerAdditionalSoapEndpoint('endpoint4')
        def endpointProps = new Properties()
        endpointProps.put('endpoint1','http://host1.com')
        endpointProps.put('endpoint2','http://host2.com')
        endpointProps.put('endpoint3','http://host3.com')
        endpointProps.put('endpoint4','http://host4.com')
        BaseDependencyInjectorAdaptor.ENDPOINT_REGISTRY=new PropertiesEndpointRegistry(endpointProps)
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
