package com.sbg.bdd.wiremock.scoped.filter;

import java.net.URL;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * NB! Cannot use CDI Singleton because it is used from a CDI extension during CDI startup
 */
public class KnownEndpointRegistry {
    private static KnownEndpointRegistry INSTANCE = new KnownEndpointRegistry();
    private static String NULL_VALUE = new String();
    private Map<String, String> soapEndpointPropertyNames = new ConcurrentHashMap<>();
    private Set<String> wireMockBaseUrls = new ConcurrentSkipListSet<>();
    private Map<String, String> restEndpointPropertyNames = new ConcurrentHashMap<>();

    public void registerSoapEndpoint(String name) {
        soapEndpointPropertyNames.put(name, NULL_VALUE);
    }

    public void registerRestEndpoint(String name) {
        restEndpointPropertyNames.put(name, NULL_VALUE);
    }

    public void registerTransitiveSoapEndpoint(String name, String url) {
        soapEndpointPropertyNames.put(name, url);
    }

    public void registerTransitiveRestEndpoint(String name, String url) {
        restEndpointPropertyNames.put(name, url);
    }

    public String getTransitiveEndpoint(String name) {
        String val = soapEndpointPropertyNames.get(name);
        val = val == null ? restEndpointPropertyNames.get(name) : val;
        return val == NULL_VALUE ? null : val;
    }

    public void registerWireMockBaseUrl(URL baseUrl) {
        wireMockBaseUrls.add(baseUrl.toExternalForm());
    }

    public static KnownEndpointRegistry getInstance() {
        return INSTANCE;
    }

    public static void clear() {
        INSTANCE = new KnownEndpointRegistry();
    }

    public boolean isNewWireMock(URL baseUrl) {
        return !wireMockBaseUrls.contains(baseUrl.toExternalForm());
    }

    public Set<String> getSoapEndpointProperties() {
        return soapEndpointPropertyNames.keySet();
    }

    public Set<String> getRestEndpointProperties() {
        return restEndpointPropertyNames.keySet();
    }
}
