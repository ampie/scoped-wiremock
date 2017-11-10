package com.sbg.bdd.wiremock.scoped.resteasy;


import com.sbg.bdd.wiremock.scoped.integration.*;
import org.jboss.resteasy.client.ClientResponse;
import org.jboss.resteasy.spi.interception.ClientExecutionContext;
import org.jboss.resteasy.spi.interception.ClientExecutionInterceptor;

import javax.ws.rs.core.MultivaluedMap;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;


class OutboundCorrelationPathRestInterceptor implements ClientExecutionInterceptor {
    Logger LOGGER = Logger.getLogger(OutboundCorrelationPathRestInterceptor.class.getName());
    @Override
    public ClientResponse execute(ClientExecutionContext ctx) throws Exception {
        RuntimeCorrelationState currentCorrelationState = DependencyInjectionAdaptorFactory.getAdaptor().getCurrentCorrelationState();
        if (currentCorrelationState.isSet()) {
            MultivaluedMap<String, Object> headers = ctx.getRequest().getHeadersAsObjects();
            URL originalHost=(URL) headers.getFirst(HeaderName.ofTheOriginalUrl());
            headers.remove(HeaderName.ofTheOriginalUrl());
            URL originalUrl = URLHelper.calculateOriginalUrl(new URL(ctx.getRequest().getUri()),originalHost);
            String key = URLHelper.identifier(originalUrl,ctx.getRequest().getHttpMethod());
            headers.add(HeaderName.ofTheCorrelationKey(), currentCorrelationState.getCorrelationPath());
            headers.add(HeaderName.ofTheOriginalUrl(), originalUrl.toExternalForm());
            headers.add(HeaderName.ofTheThreadContextId(), String.valueOf(currentCorrelationState.getCurrentThreadContextId()));
            if (currentCorrelationState.shouldProxyUnmappedEndpoints()) {
                headers.add(HeaderName.toProxyUnmappedEndpoints(), "true");
            }
            if(RuntimeCorrelationState.ON) {
                //TODO move to CorrelatedScopeAdmin
                String sequenceNumber = currentCorrelationState.getNextSequenceNumberFor(key).toString();
                headers.add(HeaderName.ofTheSequenceNumber(), sequenceNumber);
                for (ServiceInvocationCount entry : currentCorrelationState.getServiceInvocationCounts()) {
                    ctx.getRequest().header(HeaderName.ofTheServiceInvocationCount(), entry.toString());
                }
            }
        }
        ClientResponse response = ctx.proceed();
        try {
            MultivaluedMap headers = response.getHeaders();
            if (headers != null && headers.containsKey(HeaderName.ofTheServiceInvocationCount())) {
                Iterable<String> o = (Iterable<String>) headers.get(HeaderName.ofTheServiceInvocationCount());
                for (String s : o) {
                    currentCorrelationState.initSequenceNumberFor(new ServiceInvocationCount(s));
                }
            }
        } catch (Exception e) {
            //Make no assumptions
            LOGGER.log(Level.WARNING,"Could not process response", e);
        }
        return response;
    }

}
