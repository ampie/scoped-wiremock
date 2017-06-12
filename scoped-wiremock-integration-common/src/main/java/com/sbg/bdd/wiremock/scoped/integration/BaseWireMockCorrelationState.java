package com.sbg.bdd.wiremock.scoped.integration;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class BaseWireMockCorrelationState implements WireMockCorrelationState {
    private static final Logger LOGGER = Logger.getLogger(WireMockCorrelationState.class.getName());
    private String correlationPath;
    private Integer wireMockPort;
    private Map<String, Integer> sequenceNumbers = new HashMap<>();
    private boolean proxyUnmappedEndpoints = false;
    private String wireMockHost;

    public BaseWireMockCorrelationState() {
    }

    public String getCorrelationPath() {
        return correlationPath;
    }

    @Override
    public Map<String, Integer> getSequenceNumbers() {
        return Collections.unmodifiableMap(sequenceNumbers);
    }

    @Override
    public URL getWireMockBaseUrl() {
        try {
            return new URL("http://" + wireMockHost + ":" + wireMockPort);
        } catch (MalformedURLException e) {
            throw new IllegalStateException(e);
        }
    }

    //TODO perhaps retrieve this form the Automation service? - performance may be an issue
    public Integer getNextSequenceNumberFor(String endPointIdentifier) {
        Integer next = sequenceNumbers.get(endPointIdentifier);
        next = next == null ? 1 : next + 1;
        sequenceNumbers.put(endPointIdentifier, next);
        return next;
    }


    @Override
    public void set(String correlationPath, boolean proxyUnmappedEndpoints) {
        this.correlationPath = correlationPath;
        String[] split = correlationPath.split("/");
        this.wireMockPort = Integer.parseInt(split[1]);
        this.proxyUnmappedEndpoints = proxyUnmappedEndpoints;
        this.wireMockHost = split[0];
    }

    @Override
    public void clear() {
        wireMockPort = null;
        correlationPath = null;
        sequenceNumbers.clear();
        proxyUnmappedEndpoints=false;
    }

    public boolean isSet() {
        return correlationPath != null;
    }

    @Override
    public void initSequenceNumberFor(String endPointIdentifier, int count) {
        sequenceNumbers.put(endPointIdentifier, count);
    }

    public boolean shouldProxyUnmappedEndpoints() {
        return this.proxyUnmappedEndpoints;
    }

}
