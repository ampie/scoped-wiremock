package com.sbg.bdd.wiremock.scoped.filter;


import com.sbg.bdd.wiremock.scoped.integration.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.URL;
import java.util.Enumeration;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CorrelationStateSynchronizer {
    private static final Logger LOGGER = Logger.getLogger(CorrelationStateSynchronizer.class.getName());
    private WireMockCorrelationState wireMockCorrelationState;


    public CorrelationStateSynchronizer() {
        this.wireMockCorrelationState = DependencyInjectionAdaptorFactory.getAdaptor().getCurrentCorrelationState();
    }

    private void registerDefaultEndpointMappings() {
        Set<EndpointConfig> allEndpointConfigs = ServerSideEndPointConfigRegistry.getInstance().getAllEndpointConfigs();
        for (EndpointConfig config : allEndpointConfigs) {
            registerProxyMappingForEndpoint(config);
        }
        ServerSideEndPointConfigRegistry.getInstance().registerWireMockBaseUrl(this.wireMockCorrelationState.getWireMockBaseUrl());
    }


    private boolean shouldRegisterDefaultEndpointMappings() {
        return ServerSideEndPointConfigRegistry.getInstance().isNewWireMock(this.wireMockCorrelationState.getWireMockBaseUrl());
    }
    @Deprecated
    //Move to wiremock on the server
    private void registerProxyMappingForEndpoint(EndpointConfig config) {
        try {
            URL wireMockBaseUrl = new URL(this.wireMockCorrelationState.getWireMockBaseUrl() + "/__admin/mappings");
            String object = ProxyMappingBuilder.buildMapping(config);
            HttpCommandExecutor.INSTANCE.execute(new HttpCommand(wireMockBaseUrl, "POST", object));

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, config.toJson());
            LOGGER.log(Level.WARNING, "Could not register proxy mapping", e);
        }
    }


    public void readCorrelationSessionFrom(HttpServletRequest request) {
        String correlationKey = request.getHeader(HeaderName.ofTheCorrelationKey());
        if (correlationKey != null && correlationKey.length() > 0) {
            boolean proxyUnMappedEndpoints = "true".equals(request.getHeader(HeaderName.toProxyUnmappedEndpoints()));
            int threadContextId = determineThreadContextId(request);
            this.wireMockCorrelationState.set(correlationKey, threadContextId, proxyUnMappedEndpoints);
            if (proxyUnMappedEndpoints && shouldRegisterDefaultEndpointMappings()) {
                registerDefaultEndpointMappings();
            }
            Enumeration<String> headers = request.getHeaders(HeaderName.ofTheServiceInvocationCount());
            while (headers.hasMoreElements()) {
                this.wireMockCorrelationState.initSequenceNumberFor(new ServiceInvocationCount(headers.nextElement()));
            }
        } else {
            clearCorrelationSession();
        }
    }

    private int determineThreadContextId(HttpServletRequest request) {
        String threadContextIdString = request.getHeader(HeaderName.ofTheThreadContextId());
        int threadContextId=1;
        if(threadContextIdString!=null) {
            try {
                threadContextId = Integer.parseInt(threadContextIdString);
            } catch (NumberFormatException e) {

            }
        }
        return threadContextId;
    }

    public void clearCorrelationSession() {
        this.wireMockCorrelationState.clear();

    }

    public void maybeWriteCorrelationSessionTo(HttpServletResponse response) {
        response.setHeader(HeaderName.ofTheCorrelationKey(), this.wireMockCorrelationState.getCorrelationPath());
        for (ServiceInvocationCount entry : this.wireMockCorrelationState.getServiceInvocationCounts()) {
            response.addHeader(HeaderName.ofTheServiceInvocationCount(), entry.toString());
        }
    }
}
