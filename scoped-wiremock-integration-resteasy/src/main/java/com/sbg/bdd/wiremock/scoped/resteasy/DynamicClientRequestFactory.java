package com.sbg.bdd.wiremock.scoped.resteasy;


import com.sbg.bdd.wiremock.scoped.cdi.annotations.EndPointProperty;
import com.sbg.bdd.wiremock.scoped.integration.DependencyInjectionAdaptorFactory;
import com.sbg.bdd.wiremock.scoped.integration.EndPointRegistry;
import com.sbg.bdd.wiremock.scoped.integration.WireMockCorrelationState;
import org.jboss.resteasy.client.ClientExecutor;
import org.jboss.resteasy.client.ClientRequest;
import org.jboss.resteasy.client.ClientRequestFactory;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.logging.Level;

public class DynamicClientRequestFactory extends ClientRequestFactory {
    private final EndPointRegistry endpointRegistry;
    private final EndPointProperty endPointProperty;

    public DynamicClientRequestFactory(ClientExecutor executor, EndPointProperty endPointProperty) {
        super(executor, (URI) null);
        this.endpointRegistry = DependencyInjectionAdaptorFactory.getAdaptor().getEndpointRegistry();
        this.endPointProperty = endPointProperty;
        super.getPrefixInterceptors().registerInterceptor(new OutboundCorrelationPathRestInterceptor());
    }

    @Override
    public ClientRequest createRelativeRequest(String uriTemplate) {
        URL url = endpointRegistry.endpointUrlFor(endPointProperty.value());
        WireMockCorrelationState currentCorrelationState = DependencyInjectionAdaptorFactory.getAdaptor().getCurrentCorrelationState();
        if (currentCorrelationState.isSet()) {
            try {
                url = new URL(currentCorrelationState.getWireMockBaseUrl() + url.getFile() + (url.getQuery() == null ? "" : url.getQuery()));
            } catch (MalformedURLException e) {
                throw new IllegalStateException(e);
            }
        }
        return this.createRequest(url + uriTemplate);
    }

    @Override
    public ClientRequest createRequest(String uriTemplate) {
        return super.createRequest(uriTemplate);
    }
}
