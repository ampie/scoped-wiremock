package com.sbg.bdd.wiremock.scoped.filter;

import java.net.URL;

public class EndpointConfig implements Comparable<EndpointConfig> {
    public static final String NO_CATEGORY = "NONE";
    private final String category;

    public enum EndpointType {
        SOAP, UNKOWN, REST
    }

    private String propertyName;
    private URL url;
    private EndpointType endpointType;

    public EndpointConfig(String propertyName, URL url, EndpointType endpointType, String category) {
        this.propertyName = propertyName;
        this.url = url;
        this.endpointType = endpointType;
        this.category = category;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public URL getUrl() {
        return url;
    }

    public EndpointType getEndpointType() {
        return endpointType;
    }

    public String toJson() {
        return "{\"propertyName\":\"" + propertyName + "\",\"url\":\"" + url + "\",\"endpointType\":\"" + endpointType.name() + "\",\"category\":\"" + category + "\"}";

    }

    @Override
    public int compareTo(EndpointConfig o) {
        return propertyName.compareTo(o.propertyName);
    }

}
