package com.sbg.bdd.wiremock.scoped.filter;


import com.sbg.bdd.wiremock.scoped.integration.DependencyInjectionAdaptorFactory;
import com.sbg.bdd.wiremock.scoped.integration.EndPointRegistry;
import com.sbg.bdd.wiremock.scoped.integration.HeaderName;
import com.sbg.bdd.wiremock.scoped.integration.WireMockCorrelationState;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.MalformedURLException;
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
        Set<EndpointConfig> allEndpointConfigs = EndpointTypeTracker.getInstance().getAllEndpointConfigs();
        for (EndpointConfig config : allEndpointConfigs) {
            registerProxyMappingForEndpoint(config);
        }
        EndpointTypeTracker.getInstance().registerWireMockBaseUrl(this.wireMockCorrelationState.getWireMockBaseUrl());
    }


    private boolean shouldRegisterDefaultEndpointMappings() {
        return EndpointTypeTracker.getInstance().isNewWireMock(this.wireMockCorrelationState.getWireMockBaseUrl());
    }

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
            this.wireMockCorrelationState.set(correlationKey, proxyUnMappedEndpoints);
            if (proxyUnMappedEndpoints && shouldRegisterDefaultEndpointMappings()) {
                registerDefaultEndpointMappings();
            }
            Enumeration<String> headers = request.getHeaders(HeaderName.ofTheServiceInvocationCount());
            while (headers.hasMoreElements()) {
                String[] split = headers.nextElement().split("\\|");
                this.wireMockCorrelationState.initSequenceNumberFor(split[0], Integer.valueOf(split[1]));
            }
        } else {
            clearCorrelationSession();
        }
    }

    public void clearCorrelationSession() {
        this.wireMockCorrelationState.clear();

    }

    public void maybeWriteCorrelationSessionTo(HttpServletResponse response) {
        response.setHeader(HeaderName.ofTheCorrelationKey(), this.wireMockCorrelationState.getCorrelationPath());
        for (Map.Entry<String, Integer> entry : this.wireMockCorrelationState.getSequenceNumbers().entrySet()) {
            response.addHeader(HeaderName.ofTheServiceInvocationCount(), entry.getKey() + "|" + entry.getValue());
        }
    }
}
