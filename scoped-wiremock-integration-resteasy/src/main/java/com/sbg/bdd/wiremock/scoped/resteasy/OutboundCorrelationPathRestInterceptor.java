package com.sbg.bdd.wiremock.scoped.resteasy;


import com.sbg.bdd.wiremock.scoped.integration.DependencyInjectionAdaptorFactory;
import com.sbg.bdd.wiremock.scoped.integration.HeaderName;
import com.sbg.bdd.wiremock.scoped.integration.WireMockCorrelationState;
import org.jboss.resteasy.client.ClientResponse;
import org.jboss.resteasy.spi.interception.ClientExecutionContext;
import org.jboss.resteasy.spi.interception.ClientExecutionInterceptor;

import javax.ws.rs.core.MultivaluedMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;


class OutboundCorrelationPathRestInterceptor implements ClientExecutionInterceptor {
    Logger LOGGER = Logger.getLogger(OutboundCorrelationPathRestInterceptor.class.getName());
    @Override
    public ClientResponse execute(ClientExecutionContext ctx) throws Exception {
        WireMockCorrelationState currentCorrelationState = DependencyInjectionAdaptorFactory.getAdaptor().getCurrentCorrelationState();
        if (currentCorrelationState.isSet()) {
            ctx.getRequest().getHeadersAsObjects().add(HeaderName.ofTheCorrelationKey(), currentCorrelationState.getCorrelationPath());
            String key = ctx.getRequest().getUri() + ctx.getRequest().getHttpMethod();
            ctx.getRequest().getHeadersAsObjects().add(HeaderName.ofTheSequenceNumber(), currentCorrelationState.getNextSequenceNumberFor(key).toString());
            if (currentCorrelationState.shouldProxyUnmappedEndpoints()) {
                ctx.getRequest().getHeadersAsObjects().add(HeaderName.toProxyUnmappedEndpoints(), "true");
            }
            for (Map.Entry<String, Integer> entry : currentCorrelationState.getSequenceNumbers().entrySet()) {
                ctx.getRequest().header(HeaderName.ofTheServiceInvocationCount(), entry.getKey() + "|" + entry.getValue());
            }
        }
        ClientResponse response = ctx.proceed();
        try {
            MultivaluedMap headers = response.getHeaders();
            if (headers != null && headers.containsKey(HeaderName.ofTheServiceInvocationCount())) {
                Iterable<String> o = (Iterable<String>) headers.get(HeaderName.ofTheServiceInvocationCount());
                for (String s : o) {
                    String[] split = s.split("\\|");
                    currentCorrelationState.initSequenceNumberFor(split[0], Integer.valueOf(split[1]));
                }
            }
        } catch (Exception e) {
            //Make no assumptions
            LOGGER.log(Level.WARNING,"Could not process response", e);
        }
        return response;
    }
}
