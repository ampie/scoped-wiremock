package com.sbg.bdd.wiremock.scoped.server.junit;

import com.sbg.bdd.wiremock.scoped.admin.endpointconfig.RemoteEndpointConfigRegistry;
import com.sbg.bdd.wiremock.scoped.integration.BaseDependencyInjectorAdaptor;
import com.sbg.bdd.wiremock.scoped.integration.EndpointConfig;

import java.net.URL;

public class InMemoryEndpointConfigRegistry extends RemoteEndpointConfigRegistry {
    public EndpointConfig endpointConfigFor(String propertyName) {
        URL url = BaseDependencyInjectorAdaptor.ENDPOINT_REGISTRY.endpointUrlFor(propertyName);
        EndpointConfig endpointConfig = new EndpointConfig(propertyName, EndpointConfig.EndpointType.UNKOWN);
        endpointConfig.setUrl(url);
        return endpointConfig;
    }
}
