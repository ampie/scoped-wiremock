package com.sbg.bdd.wiremock.scoped.jaxrs;

import com.sbg.bdd.wiremock.scoped.integration.DependencyInjectionAdaptorFactory;
import com.sbg.bdd.wiremock.scoped.integration.HeaderName;
import com.sbg.bdd.wiremock.scoped.integration.WireMockCorrelationState;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.net.URL;
import java.util.Map;

@Provider
public class OutboundRequestCorrelationKeyFilter implements ClientRequestFilter {

    @Override
    public void filter(ClientRequestContext ctx) throws IOException {
        WireMockCorrelationState currentCorrelationState = DependencyInjectionAdaptorFactory.getAdaptor().getCurrentCorrelationState();
        if (currentCorrelationState.isSet()) {
            ctx.getHeaders().add(HeaderName.ofTheCorrelationKey(), currentCorrelationState.getCorrelationPath());
            URL url = ctx.getUri().toURL();
            String key = url.getProtocol() +"://" +  url.getAuthority() + url.getPath() + ctx.getMethod();
            ctx.getHeaders().add(HeaderName.ofTheSequenceNumber(), currentCorrelationState.getNextSequenceNumberFor(key).toString());
            if (currentCorrelationState.shouldProxyUnmappedEndpoints()) {
                ctx.getHeaders().add(HeaderName.toProxyUnmappedEndpoints(), "true");
            }
            for (Map.Entry<String, Integer> entry : currentCorrelationState.getSequenceNumbers().entrySet()) {

                ctx.getHeaders().add(HeaderName.ofTheServiceInvocationCount(), entry.getKey() + "|" + entry.getValue());
            }
        }
    }
}