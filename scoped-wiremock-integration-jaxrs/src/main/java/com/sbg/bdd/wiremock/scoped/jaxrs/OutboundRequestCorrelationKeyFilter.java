package com.sbg.bdd.wiremock.scoped.jaxrs;

import com.sbg.bdd.wiremock.scoped.integration.*;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.net.URL;

@Provider
public class OutboundRequestCorrelationKeyFilter implements ClientRequestFilter {

    @Override
    public void filter(ClientRequestContext ctx) throws IOException {
        RuntimeCorrelationState currentCorrelationState = DependencyInjectionAdaptorFactory.getAdaptor().getCurrentCorrelationState();
        if (currentCorrelationState.isSet()) {
            MultivaluedMap<String, Object> headers = ctx.getHeaders();
            URL currentUrl = ctx.getUri().toURL();
            URL originalHost=new URL(headers.getFirst(HeaderName.ofTheOriginalUrl()).toString());
            headers.remove(HeaderName.ofTheOriginalUrl());
            URL originalUrl= URLHelper.calculateOriginalUrl(currentUrl, originalHost);
            String key = URLHelper.identifier(originalUrl,ctx.getMethod());

            headers.add(HeaderName.ofTheOriginalUrl(), originalUrl.toExternalForm());
            headers.add(HeaderName.ofTheThreadContextId(), currentCorrelationState.getCurrentThreadContextId());
            headers.add(HeaderName.ofTheCorrelationKey(), currentCorrelationState.getCorrelationPath());
            if (currentCorrelationState.shouldProxyUnmappedEndpoints()) {
                headers.add(HeaderName.toProxyUnmappedEndpoints(), "true");
            }
        }
    }
}