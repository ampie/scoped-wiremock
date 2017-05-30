package com.sbg.bdd.wiremock.scoped.recording.endpointconfig;

import java.util.Set;

public interface EndpointConfigRegistry {

    EndpointConfig endpointUrlFor(String serviceEndpointPropertyName);


    Set<EndpointConfig> allKnownExternalEndpoints();
}
