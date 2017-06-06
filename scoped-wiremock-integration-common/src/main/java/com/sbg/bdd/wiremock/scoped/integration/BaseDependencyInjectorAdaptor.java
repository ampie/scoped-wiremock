package com.sbg.bdd.wiremock.scoped.integration;

import java.util.Properties;

/**
 * For tests or any other environment where DependencyInjection is not a requirement
 */
public class BaseDependencyInjectorAdaptor implements DependencyInjectorAdaptor {
    public static final Properties PROPERTIES = new Properties();
    public static EndPointRegistry ENDPOINT_REGISTRY=new PropertiesEndpointRegistry(PROPERTIES);
    public static WireMockCorrelationState CURRENT_CORRELATION_STATE=new BaseWireMockCorrelationState();

    @Override
    public WireMockCorrelationState getCurrentCorrelationState() {
        return CURRENT_CORRELATION_STATE;
    }

    @Override
    public EndPointRegistry getEndpointRegistry() {
        return ENDPOINT_REGISTRY;
    }

}
