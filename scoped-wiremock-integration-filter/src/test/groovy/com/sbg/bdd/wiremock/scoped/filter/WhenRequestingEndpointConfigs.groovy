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

class WhenRequestingEndpointConfigs extends Specification{
    def 'it should respond with a single endpoint when requested'(){

        given:
        DependencyInjectionAdaptorFactory.useAdapter(new BaseDependencyInjectorAdaptor())
        BaseDependencyInjectorAdaptor.CURRENT_CORRELATION_STATE=new BaseWireMockCorrelationState()
        BaseDependencyInjectorAdaptor.ENDPOINT_REGISTRY=Mock(EndPointRegistry){
            endpointUrlFor('some.url.prop.name') >> new URL('http://some.host.com')
        }
        def filter = new InboundCorrelationPathFilter()
        filter.init(null)
        EndpointTypeTracker.instance.registerAdditionalRestEndpoint('some.url.prop.name','category1')
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
        output.toString() == '{"propertyName":"some.url.prop.name","url":"http://some.host.com","endpointType":"REST","category":"category1"}'


    }
    def 'it should respond with al endpoints when requested'(){
        given:
        DependencyInjectionAdaptorFactory.useAdapter(new BaseDependencyInjectorAdaptor())
        BaseDependencyInjectorAdaptor.CURRENT_CORRELATION_STATE=new BaseWireMockCorrelationState()
        EndpointTypeTracker.getInstance().registerRestEndpoint('endpoint1','category1')
        EndpointTypeTracker.getInstance().registerSoapEndpoint('endpoint2','category1')
        EndpointTypeTracker.getInstance().registerAdditionalRestEndpoint('endpoint3','category2')
        EndpointTypeTracker.getInstance().registerAdditionalSoapEndpoint('endpoint4','category2')
        BaseDependencyInjectorAdaptor.ENDPOINT_REGISTRY=Mock(EndPointRegistry){
            endpointUrlFor('endpoint1') >> new URL('http://host1.com')
            endpointUrlFor('endpoint2') >> new URL('http://host2.com')
            endpointUrlFor('endpoint3') >> new URL('http://host3.com')
            endpointUrlFor('endpoint4') >> new URL('http://host4.com')
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
        output.toString() == '{"configs":[' +
                '{"propertyName":"endpoint1","url":"http://host1.com","endpointType":"REST","category":"category1"},' +
                '{"propertyName":"endpoint2","url":"http://host2.com","endpointType":"SOAP","category":"category1"},' +
                '{"propertyName":"endpoint3","url":"http://host3.com","endpointType":"REST","category":"category2"},' +
                '{"propertyName":"endpoint4","url":"http://host4.com","endpointType":"SOAP","category":"category2"}' +
                ']}'

    }
}
