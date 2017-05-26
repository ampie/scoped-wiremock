package com.sbg.bdd.wiremock.scoped.jaxws

import com.sbg.bdd.wiremock.scoped.integration.BaseDependencyInjectorAdaptor
import com.sbg.bdd.wiremock.scoped.integration.BaseWireMockCorrelationState
import com.sbg.bdd.wiremock.scoped.integration.DependencyInjectionAdaptorFactory
import com.sbg.bdd.wiremock.scoped.integration.HeaderName
import spock.lang.Specification

import javax.xml.ws.handler.MessageContext
import javax.xml.ws.handler.soap.SOAPMessageContext

class WhenCallingDownstreamJAXWSServicesFromASCope extends Specification {
    def 'the outgoing message should have the correlationPath, the sequence number and service invocation counts headers'() {

        given:
        DependencyInjectionAdaptorFactory.useAdapter(new BaseDependencyInjectorAdaptor())
        BaseDependencyInjectorAdaptor.CURRENT_CORRELATION_STATE = new BaseWireMockCorrelationState()
        BaseDependencyInjectorAdaptor.CURRENT_CORRELATION_STATE.set('localhost/8080/myscope', true)
        def handler = new OutboundCorrelationPathSOAPHandler()
        def headers = null
        def endpointIdentifier = 'http://endpoint.com/context/service/operation'
        def endpoint1 = 'http://endpoint.com/context/service/operation1'
        def endpoint2 = 'http://endpoint.com/context/service/operation2'
        BaseDependencyInjectorAdaptor.CURRENT_CORRELATION_STATE.initSequenceNumberFor(endpointIdentifier, 4)
        BaseDependencyInjectorAdaptor.CURRENT_CORRELATION_STATE.initSequenceNumberFor(endpoint1, 8)
        BaseDependencyInjectorAdaptor.CURRENT_CORRELATION_STATE.initSequenceNumberFor(endpoint2, 12)
        def messageContext = Mock(MessageContext) {
            put(_, _) >> { args ->
                headers = args[1]
            }
            get(MessageContext.MESSAGE_OUTBOUND_PROPERTY) >> Boolean.TRUE
            get(SOAPMessageContext.WSDL_OPERATION) >> endpointIdentifier
        }

        when:
        handler.handleMessage(messageContext)

        then:
        headers[HeaderName.ofTheCorrelationKey()][0] == 'localhost/8080/myscope'
        headers[HeaderName.ofTheSequenceNumber()][0] == '5'
        headers[HeaderName.ofTheServiceInvocationCount()][0] == endpoint1 + '|8'
        headers[HeaderName.ofTheServiceInvocationCount()][1] == endpoint2 + '|12'
        headers[HeaderName.toProxyUnmappedEndpoints()][0] == 'true'
    }

    def 'the service invocation counts header of the incoming response should be reflected in the current correlation state'() {

        given:
        DependencyInjectionAdaptorFactory.useAdapter(new BaseDependencyInjectorAdaptor())
        BaseDependencyInjectorAdaptor.CURRENT_CORRELATION_STATE = new BaseWireMockCorrelationState()
        BaseDependencyInjectorAdaptor.CURRENT_CORRELATION_STATE.set('localhost/8080/myscope', true)
        def handler = new OutboundCorrelationPathSOAPHandler()
        def endpointIdentifier = 'http://endpoint.com/context/service/operation'
        BaseDependencyInjectorAdaptor.CURRENT_CORRELATION_STATE.initSequenceNumberFor(endpointIdentifier, 4)
        def endpoint1 = 'http://endpoint.com/context/service/operation1'
        def endpoint2 = 'http://endpoint.com/context/service/operation2'
        def headers = new HashMap()
        headers[HeaderName.ofTheServiceInvocationCount()] = Arrays.asList(endpoint1 + '|11', endpoint2 + '|14')
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
