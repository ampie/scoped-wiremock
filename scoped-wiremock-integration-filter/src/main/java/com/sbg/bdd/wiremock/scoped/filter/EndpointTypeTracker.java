package com.sbg.bdd.wiremock.scoped.filter;

import com.sbg.bdd.wiremock.scoped.integration.DependencyInjectionAdaptorFactory;

import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
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
    private Set<String> soapEndpointPropertyNames = new ConcurrentSkipListSet<>();
    private Set<String> wireMockBaseUrls = new ConcurrentSkipListSet<>();
    private Set<String> restEndpointPropertyNames = new ConcurrentSkipListSet<>();

    public void registerSoapEndpoint(String name) {
        soapEndpointPropertyNames.add(name);
    }

    public void registerRestEndpoint(String name) {
        restEndpointPropertyNames.add(name);
    }

    public void registerAdditionalSoapEndpoint(String name) {
        soapEndpointPropertyNames.add(name);
    }

    public void registerAdditionalRestEndpoint(String name) {
        restEndpointPropertyNames.add(name);
    }

    public Set<EndpointConfig> getAllEndpointConfigs() {
        Set<EndpointConfig> result = new TreeSet<>();
        addEndpointConfigs(soapEndpointPropertyNames, result);
        addEndpointConfigs(restEndpointPropertyNames, result);
        return result;
    }

    private void addEndpointConfigs(Set<String> endpointProperties, Set<EndpointConfig> result) {
        for (String endpointProperty : endpointProperties) {
            URL url = DependencyInjectionAdaptorFactory.getAdaptor().getEndpointRegistry().endpointUrlFor(endpointProperty);
            if (url != null) {
                result.add(new EndpointConfig(endpointProperty, url, getEndpointTypeOf(endpointProperty)));
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

    public EndpointConfig.EndpointType getEndpointTypeOf(String propertyName) {
        if (restEndpointPropertyNames.contains(propertyName)) {
            return EndpointConfig.EndpointType.REST;
        } else if (soapEndpointPropertyNames.contains(propertyName)) {
            return EndpointConfig.EndpointType.SOAP;
        }
        return EndpointConfig.EndpointType.UNKOWN;
    }

    public EndpointConfig getEndpointConfig(String endpointProperty) {
        URL url = DependencyInjectionAdaptorFactory.getAdaptor().getEndpointRegistry().endpointUrlFor(endpointProperty);
        if (url != null) {
            return new EndpointConfig(endpointProperty, url, getEndpointTypeOf(endpointProperty));
        }
        return null;
    }
}
