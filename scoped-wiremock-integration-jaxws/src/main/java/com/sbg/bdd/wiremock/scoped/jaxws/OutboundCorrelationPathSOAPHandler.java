package com.sbg.bdd.wiremock.scoped.jaxws;


import com.sbg.bdd.wiremock.scoped.integration.HeaderName;
import com.sbg.bdd.wiremock.scoped.integration.WireMockCorrelationState;
import com.sbg.bdd.wiremock.scoped.integration.DependencyInjectionAdaptorFactory;

import javax.xml.namespace.QName;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.soap.SOAPMessageContext;
import java.util.*;


public class OutboundCorrelationPathSOAPHandler implements SOAPHandler {


    @Override
    public Set<QName> getHeaders() {
        return null;
    }

    @Override
    public boolean handleMessage(MessageContext context) {
        WireMockCorrelationState currentCorrelationState = DependencyInjectionAdaptorFactory.getCurrentCorrelationState();
        if (currentCorrelationState.isSet()) {
            if (Boolean.TRUE.equals(context.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY))) {
                prepareOutgoingCall(context, currentCorrelationState);
            } else {
                processIncomingResponse(context, currentCorrelationState);
            }
        }
        return true;
    }

    private void processIncomingResponse(MessageContext context, WireMockCorrelationState currentCorrelationState) {
        Map<String, List<String>> headers = (Map<String, List<String>>) context.get(SOAPMessageContext.HTTP_RESPONSE_HEADERS);
        if (headers != null && headers.get(HeaderName.ofTheServiceInvocationCount()) != null) {
            List<String> sequenceNumbers = headers.get(HeaderName.ofTheServiceInvocationCount());
            for (String entry : sequenceNumbers) {
                String[] split = entry.split("\\|");
                currentCorrelationState.initSequenceNumberFor(split[0], Integer.valueOf(split[1]));
            }
        }
    }

    private void prepareOutgoingCall(MessageContext context, WireMockCorrelationState currentCorrelationState) {
        Map<String, List<String>> headers = (Map<String, List<String>>) context.get(SOAPMessageContext.HTTP_REQUEST_HEADERS);
        if (headers == null) {
            headers = new HashMap<>();
            context.put(SOAPMessageContext.HTTP_REQUEST_HEADERS, headers);
        }
        if (headers.get(HeaderName.ofTheCorrelationKey()) == null) {
            //TODO check if this check is perhaps superfluous?
            headers.put(HeaderName.ofTheCorrelationKey(), Arrays.asList(currentCorrelationState.getCorrelationPath()));
            String key = context.get(SOAPMessageContext.WSDL_OPERATION).toString();
            headers.put(HeaderName.ofTheSequenceNumber(), Arrays.asList(currentCorrelationState.getNextSequenceNumberFor(key).toString()));
            String category= (String) context.get("endpointCategory");
            if(category!=null){
                headers.put(HeaderName.ofTheEndpointCategory(),Arrays.asList(category));
            }
            List<String> sequenceNumbers = new ArrayList<>();
            for (Map.Entry<String, Integer> entry : currentCorrelationState.getSequenceNumbers().entrySet()) {
                sequenceNumbers.add(entry.getKey() + "|" + entry.getValue());
            }
            headers.put(HeaderName.ofTheServiceInvocationCount(), sequenceNumbers);
            //TODO deserialize the service invocation counts
            if (currentCorrelationState.shouldProxyUnmappedEndpoints()) {
                headers.put(HeaderName.toProxyUnmappedEndpoints(), Arrays.asList("true"));
            }
        }
    }

    @Override
    public boolean handleFault(MessageContext context) {
        return false;
    }

    @Override
    public void close(MessageContext context) {

    }
}
