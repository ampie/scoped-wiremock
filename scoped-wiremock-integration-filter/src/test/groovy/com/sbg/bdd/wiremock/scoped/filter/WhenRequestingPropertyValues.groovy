package com.sbg.bdd.wiremock.scoped.filter

import com.sbg.bdd.wiremock.scoped.integration.BaseDependencyInjectorAdaptor
import com.sbg.bdd.wiremock.scoped.integration.BaseWireMockCorrelationState
import com.sbg.bdd.wiremock.scoped.integration.DependencyInjectionAdaptorFactory
import com.sbg.bdd.wiremock.scoped.integration.EndPointRegistry
import com.sbg.bdd.wiremock.scoped.integration.HeaderName
import spock.lang.Specification

import javax.servlet.FilterChain
import javax.servlet.ServletOutputStream
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class WhenRequestingPropertyValues extends Specification{
    def 'it should respond with a single endpoint when requested'(){

        given:
        DependencyInjectionAdaptorFactory.useAdapter(new BaseDependencyInjectorAdaptor())
        BaseDependencyInjectorAdaptor.CURRENT_CORRELATION_STATE=new BaseWireMockCorrelationState()
        BaseDependencyInjectorAdaptor.ENDPOINT_REGISTRY=Mock(EndPointRegistry){
            endpointUrlFor('some.url.prop.name') >> new URL('http://some.host.com')
        }
        def filter = new InboundCorrelationPathFilter()
        filter.init(null)
        def request = Mock(HttpServletRequest){
            getRequestURI() >> 'blablab/Property/some.url.prop.name'
            getHeader(HeaderName.ofTheCorrelationKey()) >> 'localhost/8080/scopepath'
        }
        def output = new StringBuilder()
        def outputStream=Mock(ServletOutputStream){
            print(_) >> {args ->
                output.append((args[0]))
            }
        }
        def response = Mock(HttpServletResponse){
            getOutputStream() >> outputStream
        }

        when:
        filter.doFilter(request,response,Mock(FilterChain))

        then:
        output.toString() == '{"propertyName":"some.url.prop.name","propertyValue":"http://some.host.com"}'


    }
    def 'it should respond with al endpoints when requested'(){
        given:
        DependencyInjectionAdaptorFactory.useAdapter(new BaseDependencyInjectorAdaptor())
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
            getRequestURI() >> 'blablab/Property/all'
            getHeader(HeaderName.ofTheCorrelationKey()) >> 'localhost/8080/scopepath'
        }
        def output = new StringBuilder()
        def outputStream=Mock(ServletOutputStream){
            print(_) >> {args ->
                output.append((args[0]))
            }
        }
        def response = Mock(HttpServletResponse){
            getOutputStream() >> outputStream
        }

        when:
        filter.doFilter(request,response,Mock(FilterChain))

        then:
        output.toString() == '{"properties":[' +
                '{"propertyName":"endpoint2","propertyValue":"http://host2.com"},' +
                '{"propertyName":"endpoint4","propertyValue":"http://host4.com"},' +
                '{"propertyName":"endpoint1","propertyValue":"http://host1.com"},' +
                '{"propertyName":"endpoint3","propertyValue":"http://host3.com"}]}'


    }
}
