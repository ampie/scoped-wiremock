package com.sbg.bdd.wiremock.scoped.resteasy;


import com.sbg.bdd.wiremock.scoped.cdi.annotations.EndPointCategory;
import com.sbg.bdd.wiremock.scoped.cdi.annotations.EndPointProperty;
import com.sbg.bdd.wiremock.scoped.integration.DependencyInjectionAdaptorFactory;
import com.sbg.bdd.wiremock.scoped.integration.EndPointRegistry;
import com.sbg.bdd.wiremock.scoped.integration.HeaderName;
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
    private final EndPointCategory endPointCategory;

    public DynamicClientRequestFactory(ClientExecutor executor, EndPointProperty endPointProperty,EndPointCategory endPointCategory) {
        super(executor, (URI) null);
        this.endPointCategory = endPointCategory;
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

        ClientRequest request = this.createRequest(url + uriTemplate);
        if(endPointCategory==null){
            return request;
        }
        return request.header(HeaderName.ofTheEndpointCategory(),endPointCategory.value());
    }

    @Override
    public ClientRequest createRequest(String uriTemplate) {
        return super.createRequest(uriTemplate);
    }
}
