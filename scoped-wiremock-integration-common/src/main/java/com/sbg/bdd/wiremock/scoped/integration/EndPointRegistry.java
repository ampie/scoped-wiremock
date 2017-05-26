package com.sbg.bdd.wiremock.scoped.integration;

import java.net.URL;
import java.util.Map;

public interface EndPointRegistry {

    URL endpointUrlFor(String serviceEndpointPropertyName);


    Map<String, String> allKnownExternalEndpoints();

}
