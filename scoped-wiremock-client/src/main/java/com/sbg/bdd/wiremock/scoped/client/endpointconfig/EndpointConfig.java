package com.sbg.bdd.wiremock.scoped.client.endpointconfig;

import java.net.URL;

public class EndpointConfig implements Comparable<EndpointConfig>{
    @Override
    public int compareTo(EndpointConfig o) {
        return propertyName.compareTo(o.propertyName);
    }
    public static final String NO_CATEGORY = "NONE";
    public enum EndpointType{
        SOAP, UNKOWN, REST
    }
    private String propertyName;
    private URL url;
    private EndpointType endpointType;
    private String category;

    public EndpointConfig(String propertyName, URL url, EndpointType endpointType, String category) {
        this.propertyName = propertyName;
        this.url = url;
        this.endpointType = endpointType;
        this.category = category;
    }

    public String getCategory() {
        return category;
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
}
