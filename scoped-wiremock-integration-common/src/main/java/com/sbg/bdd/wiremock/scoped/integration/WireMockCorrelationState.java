package com.sbg.bdd.wiremock.scoped.integration;

import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface WireMockCorrelationState {
    void set(String correlationPath, int threadContextId, boolean proxyUnmappedEndpoints);

    void clear();

    boolean isSet();

    String getCorrelationPath();

    Collection<ServiceInvocationCount> getServiceInvocationCounts();

    URL getWireMockBaseUrl();

    Integer getNextSequenceNumberFor(String key);

    void initSequenceNumberFor(ServiceInvocationCount serviceInvocationCount);

    boolean shouldProxyUnmappedEndpoints();

    int getCurrentThreadContextId();
}
