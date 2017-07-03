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
    private Map<String, Integer> sequenceNumbers = new HashMap<>();
    private boolean proxyUnmappedEndpoints = false;
    private Integer wireMockPort;
    private String wireMockHost;//The hostname downstream process would use to talk to wireMock
    private static String wireMockInternalHostName;//the hostname this process uses to talk to WireMock (e.g. Android: 10.0.2.2)
    public BaseWireMockCorrelationState() {
    }

    public String getCorrelationPath() {
        return correlationPath;
    }
    public static void connectToWireMockOn(String wireMockInternalHostName){
        BaseWireMockCorrelationState.wireMockInternalHostName=wireMockInternalHostName;
    }
    @Override
    public Map<String, Integer> getSequenceNumbers() {
        return Collections.unmodifiableMap(sequenceNumbers);
    }

    @Override
    public URL getWireMockBaseUrl() {
        try {
            return new URL("http://" + (wireMockInternalHostName ==null?wireMockHost:wireMockInternalHostName) + ":" + wireMockPort);
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
