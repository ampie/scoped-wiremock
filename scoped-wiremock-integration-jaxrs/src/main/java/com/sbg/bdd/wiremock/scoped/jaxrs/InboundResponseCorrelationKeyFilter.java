package com.sbg.bdd.wiremock.scoped.jaxrs;

import com.sbg.bdd.wiremock.scoped.integration.DependencyInjectionAdaptorFactory;
import com.sbg.bdd.wiremock.scoped.integration.HeaderName;
import com.sbg.bdd.wiremock.scoped.integration.RuntimeCorrelationState;
import com.sbg.bdd.wiremock.scoped.integration.ServiceInvocationCount;

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
            RuntimeCorrelationState currentCorrelationState = DependencyInjectionAdaptorFactory.getAdaptor().getCurrentCorrelationState();
            if (currentCorrelationState.isSet()) {
                MultivaluedMap headers = response.getHeaders();
                if (headers != null && headers.containsKey(HeaderName.ofTheServiceInvocationCount())) {
                    Iterable<String> o = (Iterable<String>) headers.get(HeaderName.ofTheServiceInvocationCount());
                    for (String s : o) {
                        currentCorrelationState.initSequenceNumberFor(new ServiceInvocationCount(s));
                    }
                }
            }
        } catch (Exception e) {
            //Make no assumptions
            LOGGER.log(Level.WARNING, "Could not process response", e);
        }
    }
}