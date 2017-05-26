package com.sbg.bdd.wiremock.scoped.integration;

/**
 * For tests
 */
public class BaseDependencyInjectorAdaptor implements DependencyInjectorAdaptor {
    public static EndPointRegistry ENDPOINT_REGISTRY;
    public static WireMockCorrelationState CURRENT_CORRELATION_STATE;

    @Override
    public WireMockCorrelationState getCurrentCorrelationState() {
        return CURRENT_CORRELATION_STATE;
    }

    @Override
    public EndPointRegistry getEndpointRegistry() {
        return ENDPOINT_REGISTRY;
    }

}
