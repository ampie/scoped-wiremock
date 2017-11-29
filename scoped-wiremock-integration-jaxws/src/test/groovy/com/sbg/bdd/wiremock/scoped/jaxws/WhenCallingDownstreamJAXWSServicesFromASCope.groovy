package com.sbg.bdd.wiremock.scoped.jaxws

import com.sbg.bdd.wiremock.scoped.integration.BaseDependencyInjectorAdaptor
import com.sbg.bdd.wiremock.scoped.integration.BaseRuntimeCorrelationState
import com.sbg.bdd.wiremock.scoped.integration.DependencyInjectionAdaptorFactory
import com.sbg.bdd.wiremock.scoped.integration.HeaderName
import com.sbg.bdd.wiremock.scoped.integration.RuntimeCorrelationState
import com.sbg.bdd.wiremock.scoped.integration.ServiceInvocationCount
import spock.lang.Specification

import javax.xml.ws.handler.MessageContext
import javax.xml.ws.handler.soap.SOAPMessageContext

class WhenCallingDownstreamJAXWSServicesFromASCope extends Specification {
    def 'the outgoing message should have the correlationPath, the sequence number and service invocation counts headers'() {

        given:
        DependencyInjectionAdaptorFactory.useAdaptor(new BaseDependencyInjectorAdaptor())
        BaseDependencyInjectorAdaptor.CURRENT_CORRELATION_STATE = new BaseRuntimeCorrelationState()
        BaseDependencyInjectorAdaptor.CURRENT_CORRELATION_STATE.set('localhost/8080/myscope',1, true)
        def handler = new OutboundCorrelationPathSOAPHandler()
        def headers = null
        def endpointIdentifier = 'http://endpoint.com/context/service/operation'
        def endpoint1 = 'http://endpoint.com/context/service/operation1'
        def endpoint2 = 'http://endpoint.com/context/service/operation2'
        BaseDependencyInjectorAdaptor.CURRENT_CORRELATION_STATE.initSequenceNumberFor(new ServiceInvocationCount("1|${endpointIdentifier}|4"))
        BaseDependencyInjectorAdaptor.CURRENT_CORRELATION_STATE.initSequenceNumberFor(new ServiceInvocationCount("1|${endpoint1}|8"))
        BaseDependencyInjectorAdaptor.CURRENT_CORRELATION_STATE.initSequenceNumberFor(new ServiceInvocationCount("1|${endpoint2}|12"))
        def messageContext = Mock(MessageContext) {
            put(_, _) >> { args ->
                headers = args[1]
            }
            get(HeaderName.ofTheOriginalUrl()) >> new URL('http://endpoint.com/context/service/operation')
            get(MessageContext.MESSAGE_OUTBOUND_PROPERTY) >> Boolean.TRUE
            get(SOAPMessageContext.WSDL_OPERATION) >> endpointIdentifier
            get(HeaderName.ofTheEndpointCategory()) >> ['category1']
        }

        when:
        handler.handleMessage(messageContext)

        then:
        headers[HeaderName.ofTheCorrelationKey()][0] == 'localhost/8080/myscope'
        headers[HeaderName.ofTheOriginalUrl()][0] == 'http://endpoint.com/context/service/operation'
        headers[HeaderName.toProxyUnmappedEndpoints()][0] == 'true'
        headers[HeaderName.ofTheEndpointCategory()][0] == 'category1'
        headers[HeaderName.ofTheThreadContextId()][0] == '1'
    }

    def 'the service invocation counts header of the incoming response should be reflected in the current correlation state'() {

        given:
        DependencyInjectionAdaptorFactory.useAdaptor(new BaseDependencyInjectorAdaptor())
        BaseDependencyInjectorAdaptor.CURRENT_CORRELATION_STATE = new BaseRuntimeCorrelationState()
        BaseDependencyInjectorAdaptor.CURRENT_CORRELATION_STATE.set('localhost/8080/myscope',1, true)
        def handler = new OutboundCorrelationPathSOAPHandler()
        def endpointIdentifier = 'http://endpoint.com/context/service/operation'
        BaseDependencyInjectorAdaptor.CURRENT_CORRELATION_STATE.initSequenceNumberFor(new ServiceInvocationCount("1|${endpointIdentifier}|4"))
        def endpoint1 = 'http://endpoint.com/context/service/operation1'
        def endpoint2 = 'http://endpoint.com/context/service/operation2'
        def headers = new HashMap()
        headers[HeaderName.ofTheServiceInvocationCount()] = Arrays.asList('1|' + endpoint1 + '|11', '1|' + endpoint2 + '|14')
        def messageContext = Mock(MessageContext) {
            get(SOAPMessageContext.HTTP_RESPONSE_HEADERS) >> headers
            get(MessageContext.MESSAGE_OUTBOUND_PROPERTY) >> Boolean.FALSE
        }

        when:
        handler.handleMessage(messageContext)

        then:
        BaseDependencyInjectorAdaptor.CURRENT_CORRELATION_STATE.getNextSequenceNumberFor(endpoint1) == 12
        BaseDependencyInjectorAdaptor.CURRENT_CORRELATION_STATE.getNextSequenceNumberFor(endpoint2) == 15
    }
}
