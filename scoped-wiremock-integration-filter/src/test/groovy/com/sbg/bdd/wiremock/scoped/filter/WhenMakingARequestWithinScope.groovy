package com.sbg.bdd.wiremock.scoped.filter

import com.sbg.bdd.wiremock.scoped.integration.BaseDependencyInjectorAdaptor
import com.sbg.bdd.wiremock.scoped.integration.BaseRuntimeCorrelationState
import com.sbg.bdd.wiremock.scoped.integration.DependencyInjectionAdaptorFactory
import com.sbg.bdd.wiremock.scoped.integration.HeaderName
import spock.lang.Specification

import javax.servlet.FilterChain
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class WhenMakingARequestWithinScope extends Specification{
    def 'it should respond with the updated service invocation counts'(){
        given:
        DependencyInjectionAdaptorFactory.useAdaptor(new BaseDependencyInjectorAdaptor())
        BaseDependencyInjectorAdaptor.CURRENT_CORRELATION_STATE=new BaseRuntimeCorrelationState()
        System.setProperty(InboundCorrelationPathFilter.SCOPED_WIREMOCK_ENABLED, "true")
        def filter = new InboundCorrelationPathFilter()
        filter.init(null)
        def inputServiceInvocations= new Vector()
        inputServiceInvocations << '1|http://endpoint1/|5'
        inputServiceInvocations << '1|http://endpoint2/|3'
        def request = Mock(HttpServletRequest){
            getRequestURI() >> 'nada'
            getHeader(HeaderName.ofTheCorrelationKey()) >> 'localhost/8080/scopepath'
            getHeaders(HeaderName.ofTheServiceInvocationCount()) >> inputServiceInvocations.elements()
        }
        def outputServiceInvocations= new ArrayList()
        def response = Mock(HttpServletResponse){
            addHeader(HeaderName.ofTheServiceInvocationCount(),_) >> {args ->
                outputServiceInvocations.add(args[1])
            }
        }

        when:
        filter.doFilter(request,response,Mock(FilterChain))

        then:
        inputServiceInvocations.size() == 2


    }
}
