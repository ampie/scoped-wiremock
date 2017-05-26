package com.sbg.bdd.wiremock.scoped.jaxrs;

import com.sbg.bdd.wiremock.scoped.integration.DependencyInjectionAdaptorFactory;
import com.sbg.bdd.wiremock.scoped.integration.HeaderName;
import com.sbg.bdd.wiremock.scoped.integration.WireMockCorrelationState;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

@Provider
public class InboundResponseCorrelationKeyFilter implements ClientResponseFilter {
    Logger LOGGER = Logger.getLogger(InboundResponseCorrelationKeyFilter.class.getName());

    @Override
    public void filter(ClientRequestContext requestContext, ClientResponseContext response) throws IOException {
        try {
            WireMockCorrelationState currentCorrelationState = DependencyInjectionAdaptorFactory.getAdaptor().getCurrentCorrelationState();
            if (currentCorrelationState.isSet()) {
                MultivaluedMap headers = response.getHeaders();
                if (headers != null && headers.containsKey(HeaderName.ofTheServiceInvocationCount())) {
                    Iterable<String> o = (Iterable<String>) headers.get(HeaderName.ofTheServiceInvocationCount());
                    for (String s : o) {
                        String[] split = s.split("\\|");
                        currentCorrelationState.initSequenceNumberFor(split[0], Integer.valueOf(split[1]));
                    }
                }
            }
        } catch (Exception e) {
            //Make no assumptions
            LOGGER.log(Level.WARNING, "Could not process response", e);
        }
    }
}