package com.sbg.bdd.wiremock.scoped.client.endpointconfig;


import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import okhttp3.OkHttpClient;
import okhttp3.Request;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;


public class RemoteEndPointConfigRegistry extends BaseHttpClient implements EndpointConfigRegistry {
    private final String baseUrl;
    private static final String PROPERTY_PATH = "/Property/";

    public RemoteEndPointConfigRegistry(OkHttpClient httpClient, String baseUrl) {
        super(httpClient);
        this.baseUrl = baseUrl;
    }

    @Override
    public EndpointConfig endpointUrlFor(String serviceEndpointPropertyName) {
        try {
            return toEndpointConfig(execute(new Request.Builder().url(baseUrl + PROPERTY_PATH + serviceEndpointPropertyName).get().build()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Set<EndpointConfig> allKnownExternalEndpoints() {
        try {
            ObjectNode result = execute(new Request.Builder().url(baseUrl + PROPERTY_PATH + "all").get().build());
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
        String propertyName = object.get("propertyName").asText();
        URL url = new URL(object.get("url").asText());
        EndpointConfig.EndpointType endpointType = EndpointConfig.EndpointType.valueOf(object.get("endpointType").asText());
        String category=object.get("category").asText();;
        return new EndpointConfig(propertyName, url, endpointType,category);
    }
}