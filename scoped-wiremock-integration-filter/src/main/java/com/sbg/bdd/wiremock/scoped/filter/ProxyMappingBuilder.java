package com.sbg.bdd.wiremock.scoped.filter;

import com.sbg.bdd.wiremock.scoped.integration.EndpointConfig;
import com.sbg.bdd.wiremock.scoped.integration.HeaderName;

public class ProxyMappingBuilder {
    public static String buildMapping(EndpointConfig config) {
        String path = config.getUrl().getPath();
        String proxyBase = config.getUrl().getProtocol() + "://" + config.getUrl().getAuthority();
        String url = config.getEndpointType() == EndpointConfig.EndpointType.REST? "\"urlPattern\":\"" + path + ".*\"" : "\"url\":\"" + path + "\"";
        return "{\"request\":" +
                "{\"method\":\"ANY\"," + url + "," +
                "\"headers\":{\"" + HeaderName.toProxyUnmappedEndpoints() + "\":{\"equalTo\":\"true\"}}}," +
                "\"response\":{\"proxyBaseUrl\":\"" + proxyBase + "\"}," +
                "\"priority\":201}";
    }

}
