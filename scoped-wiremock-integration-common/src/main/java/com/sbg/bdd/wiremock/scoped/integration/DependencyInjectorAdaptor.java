package com.sbg.bdd.wiremock.scoped.integration;


public interface DependencyInjectorAdaptor {
    WireMockCorrelationState getCurrentCorrelationState();
    EndPointRegistry getEndpointRegistry();
}
