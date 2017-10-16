package com.sbg.bdd.wiremock.scoped.jaxws;


import com.sbg.bdd.wiremock.scoped.integration.HeaderName;
import com.sbg.bdd.wiremock.scoped.integration.URLHelper;
import com.sbg.bdd.wiremock.scoped.integration.WireMockCorrelationState;
import com.sbg.bdd.wiremock.scoped.integration.DependencyInjectionAdaptorFactory;

import javax.xml.namespace.QName;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.soap.SOAPMessageContext;
import java.net.URL;
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
            URL originalUrl= (URL) context.get(HeaderName.ofTheOriginalUrl());
            String endpointIdentifier = URLHelper.identifier(originalUrl);
            String sequenceNumber = currentCorrelationState.getNextSequenceNumberFor(endpointIdentifier).toString();

            headers.put(HeaderName.ofTheCorrelationKey(), Arrays.asList(currentCorrelationState.getCorrelationPath()));
            headers.put(HeaderName.ofTheOriginalUrl(), Arrays.asList(originalUrl.toExternalForm()));
            headers.put(HeaderName.ofTheSequenceNumber(), Arrays.asList(sequenceNumber));
            List<String> categories= (List<String>) context.get(HeaderName.ofTheEndpointCategory());
            if(categories!=null){
                headers.put(HeaderName.ofTheEndpointCategory(),categories);
            }
            List<String> sequenceNumbers = new ArrayList<>();
            for (Map.Entry<String, Integer> entry : currentCorrelationState.getSequenceNumbers().entrySet()) {
                sequenceNumbers.add(entry.getKey() + "|" + entry.getValue());
            }
            headers.put(HeaderName.ofTheServiceInvocationCount(), sequenceNumbers);
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
