package com.sbg.bdd.wiremock.scoped.filter;


import com.sbg.bdd.wiremock.scoped.integration.DependencyInjectionAdaptorFactory;
import com.sbg.bdd.wiremock.scoped.integration.EndPointRegistry;
import com.sbg.bdd.wiremock.scoped.integration.HeaderName;
import com.sbg.bdd.wiremock.scoped.integration.WireMockCorrelationState;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.URL;
import java.util.Enumeration;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CorrelationStateSynchronizer {
    private static final Logger LOGGER = Logger.getLogger(CorrelationStateSynchronizer.class.getName());
    private WireMockCorrelationState wireMockCorrelationState;
    private EndPointRegistry endpointRegistry;


    public CorrelationStateSynchronizer(EndPointRegistry endpointRegistry) {
        this.endpointRegistry = endpointRegistry;
        this.wireMockCorrelationState= DependencyInjectionAdaptorFactory.getAdaptor().getCurrentCorrelationState();
    }

    private void registerDefaultEndpointMappings() {
        for (String s : KnownEndpointRegistry.getInstance().getSoapEndpointProperties()) {
            registerProxyMappingForEndpoint(s, false);
        }
        for (String s : KnownEndpointRegistry.getInstance().getRestEndpointProperties()) {
            registerProxyMappingForEndpoint(s, true);
        }
        KnownEndpointRegistry.getInstance().registerWireMockBaseUrl(this.wireMockCorrelationState.getWireMockBaseUrl());
    }


    private boolean shouldRegisterDefaultEndpointMappings() {
        return KnownEndpointRegistry.getInstance().isNewWireMock(this.wireMockCorrelationState.getWireMockBaseUrl());
    }

    private void registerProxyMappingForEndpoint(String epp, boolean useUrlPattern) {
        try {
            URL wireMockBaseUrl = new URL(this.wireMockCorrelationState.getWireMockBaseUrl() + "/__admin/mappings");
            URL endPointUrl = endpointRegistry.endpointUrlFor(epp);
            if(endPointUrl==null){
                String possibleValue = KnownEndpointRegistry.getInstance().getTransitiveEndpoint(epp);
                if(possibleValue!=null){
                    endPointUrl = new URL(possibleValue);
                }
            }
            if(endPointUrl!=null) {
                String object = ProxyMappingBuilder.buildMapping(endPointUrl, useUrlPattern);
                HttpCommandExecutor.INSTANCE.execute(new HttpCommand(wireMockBaseUrl, "POST", object));
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, epp);
            LOGGER.log(Level.WARNING, "Could not register proxy mapping", e);
        }
    }


    public void readCorrelationSessionFrom(HttpServletRequest request) {
        String correlationKey = request.getHeader(HeaderName.ofTheCorrelationKey());
        boolean proxyUnMappedEndpoints = "true".equals(request.getHeader(HeaderName.toProxyUnmappedEndpoints()));
        this.wireMockCorrelationState.set(correlationKey, proxyUnMappedEndpoints);
        if (proxyUnMappedEndpoints && shouldRegisterDefaultEndpointMappings()) {
            registerDefaultEndpointMappings();
        }
        Enumeration<String> headers = request.getHeaders(HeaderName.ofTheServiceInvocationCount());
        while (headers.hasMoreElements()) {
            String[] split = headers.nextElement().split("\\|");
            this.wireMockCorrelationState.initSequenceNumberFor(split[0], Integer.valueOf(split[1]));
        }
    }

    public void clearCorrelationSession() {
        this.wireMockCorrelationState.clear();

    }

    public void maybeWriteCorrelationSessionTo(HttpServletResponse response) {
        for (Map.Entry<String, Integer> entry : this.wireMockCorrelationState.getSequenceNumbers().entrySet()) {
            response.addHeader(HeaderName.ofTheServiceInvocationCount(), entry.getKey() + "|" + entry.getValue());
        }
    }
}
