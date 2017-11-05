package com.sbg.bdd.wiremock.scoped.integration;

import java.lang.reflect.Method;
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

    //Call this method once a new thread context is started and running
    void setCurrentThreadCorrelationContext(Method method, Object[] parameters);

    Integer getNextSequenceNumberFor(String key);

    void initSequenceNumberFor(ServiceInvocationCount serviceInvocationCount);

    boolean shouldProxyUnmappedEndpoints();

    int getCurrentThreadContextId();

    void newChildContext(Method method, Object[] parameters);

    void clearCurrentThreadCorrelationContext(Method method, Object[] parameters);
}
