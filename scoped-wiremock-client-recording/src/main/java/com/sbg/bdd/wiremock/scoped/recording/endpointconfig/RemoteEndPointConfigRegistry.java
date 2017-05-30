package com.sbg.bdd.wiremock.scoped.recording.endpointconfig;


import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;


public class RemoteEndPointConfigRegistry extends BaseHttpClient implements EndpointConfigRegistry {
    private final String baseUrl;
    private static final String PROPERTY_PATH = "/Property/";

    public RemoteEndPointConfigRegistry(CloseableHttpClient httpClient, String baseUrl) {
        super(httpClient);
        this.baseUrl = baseUrl;
    }

    @Override
    public EndpointConfig endpointUrlFor(String serviceEndpointPropertyName) {
        try {
            return toEndpointConfig(execute(new HttpGet(baseUrl + PROPERTY_PATH + serviceEndpointPropertyName)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Set<EndpointConfig> allKnownExternalEndpoints() {
        try {
            ObjectNode result = execute(new HttpGet(baseUrl + PROPERTY_PATH + "all"));
            ArrayNode array = (ArrayNode) result.get("configs");

            Set<EndpointConfig> endPoints=new TreeSet<>();
            for (int i = 0; i < array.size(); i++) {
                ObjectNode object = (ObjectNode) array.get(i);
                try {
                    endPoints.add(toEndpointConfig(object));
                } catch (MalformedURLException e) {
                    //ignore it
                }
            }
            return endPoints;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private EndpointConfig toEndpointConfig(ObjectNode object) throws MalformedURLException {
        return new EndpointConfig(object.get("propertyName").asText(), new URL(object.get("url").asText()), EndpointConfig.EndpointType.valueOf(object.get("endpointType").asText()));
    }
}
