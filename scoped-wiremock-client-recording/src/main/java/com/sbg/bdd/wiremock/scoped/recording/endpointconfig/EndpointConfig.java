package com.sbg.bdd.wiremock.scoped.recording.endpointconfig;

import java.net.URL;

public class EndpointConfig implements Comparable<EndpointConfig>{
    @Override
    public int compareTo(EndpointConfig o) {
        return propertyName.compareTo(o.propertyName);
    }

    public enum EndpointType{
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
}
