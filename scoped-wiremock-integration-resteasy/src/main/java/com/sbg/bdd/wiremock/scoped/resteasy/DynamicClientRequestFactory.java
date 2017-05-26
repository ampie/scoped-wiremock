package com.sbg.bdd.wiremock.scoped.resteasy;


import com.sbg.bdd.wiremock.scoped.cdi.annotations.EndPointProperty;
import com.sbg.bdd.wiremock.scoped.integration.DependencyInjectionAdaptorFactory;
import com.sbg.bdd.wiremock.scoped.integration.EndPointRegistry;
import org.jboss.resteasy.client.ClientExecutor;
import org.jboss.resteasy.client.ClientRequest;
import org.jboss.resteasy.client.ClientRequestFactory;

import java.net.URI;

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
        return this.createRequest(endpointRegistry.endpointUrlFor(endPointProperty.value()) + uriTemplate);
    }

    @Override
    public ClientRequest createRequest(String uriTemplate) {
        return super.createRequest(uriTemplate);
    }
}
