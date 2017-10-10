package com.sbg.bdd.wiremock.scoped.filter;

import com.sbg.bdd.wiremock.scoped.integration.DependencyInjectionAdaptorFactory;
import com.sbg.bdd.wiremock.scoped.integration.EndpointConfig;

import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * This provides the only mechanism to distinguish between SOAP and REST endpoints. The idea is for this info to be
 * aggregated at deployment time. E.g. for CDI, the injection target processing will pick up whether it is a WebServiceRef
 * or a REST basepath. This class also allows developers to programmatically add additional endpoints that were not
 * picked up during deployment, and this code could for instance sit in servlet ContextListener
 * Current use cases of this class are:
 * 1. When the default ProxyUnmappedEndpoints mappings are sent to WireMock this allows different url mappings for SOAP (equalTo) and REST (matching) urls
 * 2. On the client side, when building normal proxying requests, this reduces verbosity of mappings as we can know up front whether it should be a SOAP or REST mapping..
 * 3. On the client side, when specifying bulk proxying of all know endpoints, it ensure that only the endpoints that were explicitly configured for WireMock proxying will be returned
 * NB!
 * Cannot use CDI because this should work in Spring too
 * but also, cannot be a Injection Framework Singleton because it is used during startup.
 * and also, cannot use any Injection infrastructure such as com.sbg.bdd.wiremock.scoped.cdi.internal.CdiAdaptor at startup for same reason
 */
public class ServerSideEndPointConfigRegistry {
    private static ServerSideEndPointConfigRegistry INSTANCE = new ServerSideEndPointConfigRegistry();
    private Map<String, EndpointConfig> endpointConfigMap = new ConcurrentHashMap<>();
    private Set<String> wireMockBaseUrls = new ConcurrentSkipListSet<>();

    public void registerSoapEndpoint(String name, String... categories) {
        registerSoapEndpoint(name, categories, new String[0]);
    }

    public void registerRestEndpoint(String name, String... categories) {
        registerRestEndpoint(name, categories, new String[0]);
    }

    public void registerSoapEndpoint(String name, String[] categories, String[] scopes) {
        register(name, EndpointConfig.EndpointType.SOAP, categories, scopes);
    }

    public void registerRestEndpoint(String name, String[] categories, String[] scopes) {
        register(name, EndpointConfig.EndpointType.REST, categories, scopes);
    }

    public Set<EndpointConfig> getAllEndpointConfigs() {
        Set<EndpointConfig> result = new TreeSet<>();
        Collection<EndpointConfig> values = endpointConfigMap.values();
        for (EndpointConfig value : values) {
            EndpointConfig matched = matchUrl(value);
            if (matched != null) {
                result.add(matched);
            }
        }
        return result;
    }

    public void registerWireMockBaseUrl(URL baseUrl) {
        wireMockBaseUrls.add(baseUrl.toExternalForm());
    }

    public static ServerSideEndPointConfigRegistry getInstance() {
        return INSTANCE;
    }

    public static void clear() {
        INSTANCE = new ServerSideEndPointConfigRegistry();
    }

    public boolean isNewWireMock(URL baseUrl) {
        return !wireMockBaseUrls.contains(baseUrl.toExternalForm());
    }


    public EndpointConfig getEndpointConfig(String endpointProperty) {
        EndpointConfig endpointConfig = endpointConfigMap.get(endpointProperty);
        if (endpointConfig != null) {
            return matchUrl(endpointConfig);
        } else {
            return null;
        }
    }

    private EndpointConfig matchUrl(EndpointConfig endpointConfig) {
        if (endpointConfig.getUrl() == null) {
            URL url = DependencyInjectionAdaptorFactory.getAdaptor().getEndpointRegistry().endpointUrlFor(endpointConfig.getPropertyName());
            endpointConfig.setUrl(url);
        }
        if (endpointConfig.getUrl() == null) {
            return null;
        } else {
            return endpointConfig;
        }
    }

    private void register(String name, EndpointConfig.EndpointType endpointType, String[] categories, String[] scopes) {
        EndpointConfig endpointConfig = endpointConfigMap.get(name);
        if (endpointConfig == null) {
            endpointConfigMap.put(name, new EndpointConfig(name, endpointType, categories, scopes));
        }
    }
}
