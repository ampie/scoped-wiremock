package com.sbg.bdd.wiremock.scoped.resteasy;


import com.sbg.bdd.wiremock.scoped.cdi.annotations.EndpointInfo;
import com.sbg.bdd.wiremock.scoped.integration.*;
import org.jboss.resteasy.client.ClientExecutor;
import org.jboss.resteasy.client.ClientRequest;
import org.jboss.resteasy.client.ClientRequestFactory;

import java.net.URI;
import java.net.URL;

public class DynamicClientRequestFactory extends ClientRequestFactory {
    private final EndpointRegistry endpointRegistry;
    private final EndpointInfo endPointProperty;

    public DynamicClientRequestFactory(ClientExecutor executor, EndpointInfo endPointProperty) {
        super(executor, (URI) null);
        this.endpointRegistry = DependencyInjectionAdaptorFactory.getAdaptor().getEndpointRegistry();
        this.endPointProperty = endPointProperty;
        super.getPrefixInterceptors().registerInterceptor(new OutboundCorrelationPathRestInterceptor());
    }

    @Override
    public ClientRequest createRelativeRequest(String uriTemplate) {
        URL originalUrl = endpointRegistry.endpointUrlFor(endPointProperty.propertyName());
        RuntimeCorrelationState currentCorrelationState = DependencyInjectionAdaptorFactory.getAdaptor().getCurrentCorrelationState();
        URL url = originalUrl;
        if (currentCorrelationState.isSet()) {
            url = URLHelper.replaceBaseUrl(originalUrl, currentCorrelationState.getWireMockBaseUrl());
        }
        ClientRequest request = this.createRequest(url + uriTemplate);
        if (endPointProperty.categories() != null) {
            for (String s : endPointProperty.categories()) {
                request = request.header(HeaderName.ofTheEndpointCategory(), s);
            }
        }
        //This is still wrong, but the interceptor will fix it
        request = request.header(HeaderName.ofTheOriginalUrl(), URLHelper.hostOnly(originalUrl));
        return request;
    }

    @Override
    public ClientRequest createRequest(String uriTemplate) {
        return super.createRequest(uriTemplate);
    }
}
