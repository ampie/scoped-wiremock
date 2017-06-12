package com.sbg.bdd.wiremock.scoped.resteasy;


import com.sbg.bdd.wiremock.scoped.integration.DependencyInjectionAdaptorFactory;
import com.sbg.bdd.wiremock.scoped.integration.HeaderName;
import com.sbg.bdd.wiremock.scoped.integration.URLHelper;
import com.sbg.bdd.wiremock.scoped.integration.WireMockCorrelationState;
import org.jboss.resteasy.client.ClientResponse;
import org.jboss.resteasy.spi.interception.ClientExecutionContext;
import org.jboss.resteasy.spi.interception.ClientExecutionInterceptor;

import javax.ws.rs.core.MultivaluedMap;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;


class OutboundCorrelationPathRestInterceptor implements ClientExecutionInterceptor {
    Logger LOGGER = Logger.getLogger(OutboundCorrelationPathRestInterceptor.class.getName());
    @Override
    public ClientResponse execute(ClientExecutionContext ctx) throws Exception {
        WireMockCorrelationState currentCorrelationState = DependencyInjectionAdaptorFactory.getAdaptor().getCurrentCorrelationState();
        if (currentCorrelationState.isSet()) {
            MultivaluedMap<String, Object> headers = ctx.getRequest().getHeadersAsObjects();
            URL originalHost=(URL) headers.getFirst(HeaderName.ofTheOriginalUrl());
            headers.remove(HeaderName.ofTheOriginalUrl());
            URL originalUrl = URLHelper.calculateOriginalUrl(new URL(ctx.getRequest().getUri()),originalHost);
            String key = URLHelper.identifier(originalUrl,ctx.getRequest().getHttpMethod());
            String sequenceNumber = currentCorrelationState.getNextSequenceNumberFor(key).toString();

            headers.add(HeaderName.ofTheCorrelationKey(), currentCorrelationState.getCorrelationPath());
            headers.add(HeaderName.ofTheOriginalUrl(), originalUrl.toExternalForm());
            headers.add(HeaderName.ofTheSequenceNumber(), sequenceNumber);
            if (currentCorrelationState.shouldProxyUnmappedEndpoints()) {
                headers.add(HeaderName.toProxyUnmappedEndpoints(), "true");
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

    public static void main(String[] args) throws MalformedURLException {
        System.out.println(new URL("http://asdf:wer@sadfasdf:90/asdf/fsda?sadf=123").getPath());
    }
}
