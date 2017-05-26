package com.sbg.bdd.wiremock.scoped.integration;

import java.net.URL;
import java.util.Map;

public interface WireMockCorrelationState {
    void set(String correlationPath, boolean proxyUnmappedEndpoints);

    void clear();

    boolean isSet();

    String getCorrelationPath();

    Map<String, Integer> getSequenceNumbers();

    URL getWireMockBaseUrl();

    Integer getNextSequenceNumberFor(String key);

    void initSequenceNumberFor(String endPointIdentifier, int count);

    boolean shouldProxyUnmappedEndpoints();
}
