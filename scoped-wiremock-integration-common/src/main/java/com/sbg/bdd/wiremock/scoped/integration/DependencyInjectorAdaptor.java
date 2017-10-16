package com.sbg.bdd.wiremock.scoped.integration;

/**
 * A contract to specify which components/beans/services are to be resolved
 * from the underlying DI framework
 */
public interface DependencyInjectorAdaptor {
    WireMockCorrelationState getCurrentCorrelationState();
    EndpointRegistry getEndpointRegistry();
}
