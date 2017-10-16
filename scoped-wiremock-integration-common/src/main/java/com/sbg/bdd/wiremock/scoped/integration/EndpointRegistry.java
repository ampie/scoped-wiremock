package com.sbg.bdd.wiremock.scoped.integration;

import java.net.URL;
import java.util.Map;

public interface EndpointRegistry {

    URL endpointUrlFor(String serviceEndpointPropertyName);


}
