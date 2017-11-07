package com.sbg.bdd.wiremock.scoped.integration;

import java.util.Properties;

/**
 * For tests or any other environment where DependencyInjection is not a requirement
 */
public class BaseDependencyInjectorAdaptor implements DependencyInjectorAdaptor {
    public static final Properties PROPERTIES = new Properties();
    public static EndpointRegistry ENDPOINT_REGISTRY = new PropertiesEndpointRegistry(PROPERTIES);
    public static RuntimeCorrelationState CURRENT_CORRELATION_STATE = new BaseRuntimeCorrelationState();

    public BaseDependencyInjectorAdaptor() {
        ENDPOINT_REGISTRY = new PropertiesEndpointRegistry(PROPERTIES);
        CURRENT_CORRELATION_STATE = new BaseRuntimeCorrelationState();
    }

    @Override
    public RuntimeCorrelationState getCurrentCorrelationState() {
        return CURRENT_CORRELATION_STATE;
    }

    @Override
    public EndpointRegistry getEndpointRegistry() {
        return ENDPOINT_REGISTRY;
    }

}
