package com.sbg.bdd.wiremock.scoped.filter;

import com.sbg.bdd.wiremock.scoped.integration.DependencyInjectionAdaptorFactory;

import java.net.URL;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * This provides the only mechanism to distinguish between SOAP and REST endpoints. THe idea is for this info to be
 * aggregated at deployment time. E.g. for CDI, the injection target processing will pick up whether it is a WebServiceRef
 * or  a REST basepath. This class also allows developers to programmatically add additional endpoints that were not
 * picked up during deployment, and this code could for instance sit in servlet ContextListener
 * Current use cases of this class are:
 * 1. When the default ProxyUnmappedEndpoints mappings are sent to WireMock this allows different url mappings for SOAP (equalTo) and REST (matching) urls
 * 2. On the client side, when building normal proxying requests, this reduces verbosity of mappings as we can know up front whether it should be a SOAP or REST mapping..
 * 3. On the client side, when specifying bulk proxying of all know endpoints, it ensure that only the endpoints that were explicitly configured for WireMock proxying will be returned
 * NB! Cannot use CDI Singleton because it is used from a CDI extension during CDI startup
 */
public class EndpointTypeTracker {
    private static EndpointTypeTracker INSTANCE = new EndpointTypeTracker();
    private Map<String,String> soapEndpointPropertyNames = new ConcurrentHashMap<>();
    private Set<String> wireMockBaseUrls = new ConcurrentSkipListSet<>();
    private Map<String,String> restEndpointPropertyNames = new ConcurrentHashMap<>();

    public void registerSoapEndpoint(String name, String category) {
        soapEndpointPropertyNames.put(name,category);
    }

    public void registerRestEndpoint(String name, String category) {
        restEndpointPropertyNames.put(name,category);
    }

    public void registerAdditionalSoapEndpoint(String name, String category) {
        soapEndpointPropertyNames.put(name,category);
    }

    public void registerAdditionalRestEndpoint(String name, String category) {
        restEndpointPropertyNames.put(name,category);
    }

    public Set<EndpointConfig> getAllEndpointConfigs() {
        Set<EndpointConfig> result = new TreeSet<>();
        addEndpointConfigs(soapEndpointPropertyNames, result);
        addEndpointConfigs(restEndpointPropertyNames, result);
        return result;
    }

    private void addEndpointConfigs(Map<String,String> endpointProperties, Set<EndpointConfig> result) {
        for (String propertyName : endpointProperties.keySet()) {
            URL url = DependencyInjectionAdaptorFactory.getAdaptor().getEndpointRegistry().endpointUrlFor(propertyName);
            if (url != null) {
                Map.Entry<EndpointConfig.EndpointType, String> typeAndCategory = getEndpointTypeAndCategoryOf(propertyName);
                result.add(new EndpointConfig(propertyName, url, typeAndCategory.getKey(),typeAndCategory.getValue()));
            }
        }
    }

    public void registerWireMockBaseUrl(URL baseUrl) {
        wireMockBaseUrls.add(baseUrl.toExternalForm());
    }

    public static EndpointTypeTracker getInstance() {
        return INSTANCE;
    }

    public static void clear() {
        INSTANCE = new EndpointTypeTracker();
    }

    public boolean isNewWireMock(URL baseUrl) {
        return !wireMockBaseUrls.contains(baseUrl.toExternalForm());
    }

    private Map.Entry<EndpointConfig.EndpointType,String> getEndpointTypeAndCategoryOf(String propertyName) {
        final EndpointConfig.EndpointType type;
        final String category;
        if (restEndpointPropertyNames.containsKey(propertyName)) {
            type= EndpointConfig.EndpointType.REST;
            category=restEndpointPropertyNames.get(propertyName);
        } else if (soapEndpointPropertyNames.containsKey(propertyName)) {
            type= EndpointConfig.EndpointType.SOAP;
            category=soapEndpointPropertyNames.get(propertyName);
        }else{
            type= EndpointConfig.EndpointType.UNKOWN;
            category = EndpointConfig.NO_CATEGORY;
        }
        return new Map.Entry<EndpointConfig.EndpointType,String>(){

            @Override
            public EndpointConfig.EndpointType getKey() {
                return type;
            }

            @Override
            public String getValue() {
                return category;
            }

            @Override
            public String setValue(String value) {
                return null;
            }
        };
    }

    public EndpointConfig getEndpointConfig(String endpointProperty) {
        URL url = DependencyInjectionAdaptorFactory.getAdaptor().getEndpointRegistry().endpointUrlFor(endpointProperty);
        if (url != null) {
            Map.Entry<EndpointConfig.EndpointType, String> typeAndCategory = getEndpointTypeAndCategoryOf(endpointProperty);
            return new EndpointConfig(endpointProperty, url, typeAndCategory.getKey(),typeAndCategory.getValue());
        }
        return null;
    }
}
