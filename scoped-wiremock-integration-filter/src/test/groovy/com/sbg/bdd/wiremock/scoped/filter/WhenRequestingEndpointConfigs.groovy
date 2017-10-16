package com.sbg.bdd.wiremock.scoped.filter

import com.sbg.bdd.wiremock.scoped.integration.BaseDependencyInjectorAdaptor
import com.sbg.bdd.wiremock.scoped.integration.BaseWireMockCorrelationState
import com.sbg.bdd.wiremock.scoped.integration.DependencyInjectionAdaptorFactory
import com.sbg.bdd.wiremock.scoped.integration.EndpointConfig
import com.sbg.bdd.wiremock.scoped.integration.EndpointRegistry
import com.sbg.bdd.wiremock.scoped.integration.HeaderName
import spock.lang.Specification

import javax.servlet.FilterChain
import javax.servlet.ServletOutputStream
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class WhenRequestingEndpointConfigs extends Specification {
    def 'it should respond with al endpoints when requested'() {
        given:
        DependencyInjectionAdaptorFactory.useAdapter(new BaseDependencyInjectorAdaptor())
        BaseDependencyInjectorAdaptor.CURRENT_CORRELATION_STATE = new BaseWireMockCorrelationState()
        ServerSideEndPointConfigRegistry.clear()
        ServerSideEndPointConfigRegistry.getInstance().registerRestEndpoint('endpoint1', 'category1')
        ServerSideEndPointConfigRegistry.getInstance().registerSoapEndpoint('endpoint2', 'category1')
        ServerSideEndPointConfigRegistry.getInstance().registerRestEndpoint('endpoint3', 'category2')
        ServerSideEndPointConfigRegistry.getInstance().registerSoapEndpoint('endpoint4', 'category2')
        BaseDependencyInjectorAdaptor.ENDPOINT_REGISTRY = Mock(EndpointRegistry) {
            endpointUrlFor('endpoint1') >> new URL('http://host1.com')
            endpointUrlFor('endpoint2') >> new URL('http://host2.com')
            endpointUrlFor('endpoint3') >> new URL('http://host3.com')
            endpointUrlFor('endpoint4') >> new URL('http://host4.com')
        }
        def filter = new InboundCorrelationPathFilter()
        filter.init(null)
        def request = Mock(HttpServletRequest) {
            getRequestURI() >> 'http://someHost.com/blablab' + EndpointConfig.ENDPOINT_CONFIG_PATH + 'all'
            getHeader(HeaderName.ofTheCorrelationKey()) >> 'localhost/8080/scopepath'
        }
        def output = new StringBuilder()
        def outputStream = Mock(ServletOutputStream) {
            print(_) >> { args ->
                output.append((args[0]))
            }
        }
        def response = Mock(HttpServletResponse) {
            getOutputStream() >> outputStream
        }

        when:
        filter.doFilter(request, response, Mock(FilterChain))

        then:
        output.toString() == '[' +
                '{"propertyName":"endpoint1","url":"http://host1.com","endpointType":"REST","categories":["category1"],"scopes":[]},' +
                '{"propertyName":"endpoint2","url":"http://host2.com","endpointType":"SOAP","categories":["category1"],"scopes":[]},' +
                '{"propertyName":"endpoint3","url":"http://host3.com","endpointType":"REST","categories":["category2"],"scopes":[]},' +
                '{"propertyName":"endpoint4","url":"http://host4.com","endpointType":"SOAP","categories":["category2"],"scopes":[]}' +
                ']'

    }
}
