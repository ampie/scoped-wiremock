package com.sbg.bdd.wiremock.scoped.filter;

import java.net.URL;

public class EndpointConfig implements Comparable<EndpointConfig> {
    public enum EndpointType {
        SOAP, UNKOWN, REST
    }

    private String propertyName;
    private URL url;
    private EndpointType endpointType;

    public EndpointConfig(String propertyName, URL url, EndpointType endpointType) {
        this.propertyName = propertyName;
        this.url = url;
        this.endpointType = endpointType;
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
        return "{\"propertyName\":\"" + propertyName + "\",\"url\":\"" + url + "\",\"endpointType\":\"" + endpointType.name() + "\"}";

    }

    @Override
    public int compareTo(EndpointConfig o) {
        return propertyName.compareTo(o.propertyName);
    }

}
