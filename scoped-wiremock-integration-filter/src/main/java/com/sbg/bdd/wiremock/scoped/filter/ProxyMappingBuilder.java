package com.sbg.bdd.wiremock.scoped.filter;

import com.sbg.bdd.wiremock.scoped.integration.HeaderName;

import java.net.URL;

public class ProxyMappingBuilder {
    public static String buildMapping(URL endPointUrl, boolean useUrlPattern) {
        String path = endPointUrl.getPath();
        String proxyBase = endPointUrl.getProtocol() + "://" + endPointUrl.getAuthority();
        String url = useUrlPattern ? "\"urlPattern\":\"" + path + ".*\"" : "\"url\":\"" + path + "\"";
        return "{\"request\":" +
                "{\"method\":\"ANY\"," + url + "," +
                "\"headers\":{\"" + HeaderName.toProxyUnmappedEndpoints() + "\":{\"equalTo\":\"true\"}}}," +
                "\"response\":{\"proxyBaseUrl\":\"" + proxyBase + "\"}," +
                "\"priority\":201}";
    }

}
