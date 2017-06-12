package com.sbg.bdd.wiremock.scoped.resteasy;


import com.sbg.bdd.wiremock.scoped.cdi.annotations.EndPointCategory;
import com.sbg.bdd.wiremock.scoped.cdi.annotations.EndPointProperty;
import com.sbg.bdd.wiremock.scoped.integration.*;
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
        URL originalUrl = endpointRegistry.endpointUrlFor(endPointProperty.value());
        WireMockCorrelationState currentCorrelationState = DependencyInjectionAdaptorFactory.getAdaptor().getCurrentCorrelationState();
        URL url=originalUrl;
        if (currentCorrelationState.isSet()) {
            url= URLHelper.replaceBaseUrl(originalUrl,currentCorrelationState.getWireMockBaseUrl());
        }
        ClientRequest request = this.createRequest(url + uriTemplate);
        if(endPointCategory!=null){
            request=request.header(HeaderName.ofTheEndpointCategory(),endPointCategory.value());
        }
        //This is still wrong, but the interceptor will fix it
        request=request.header(HeaderName.ofTheOriginalUrl(),URLHelper.hostOnly(originalUrl));
        return request;
    }

    @Override
    public ClientRequest createRequest(String uriTemplate) {
        return super.createRequest(uriTemplate);
    }
}
