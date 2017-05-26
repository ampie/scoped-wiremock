package com.sbg.bdd.wiremock.scoped.endpointregistry;


import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sbg.bdd.wiremock.scoped.integration.EndPointRegistry;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;


public class RemoteEndPointRegistry extends BaseHttpClient implements EndPointRegistry {
    private final String baseUrl;
    private static final String PROPERTY_PATH = "/Property/";

    public RemoteEndPointRegistry(CloseableHttpClient httpClient, String baseUrl) {
        super(httpClient);
        this.baseUrl = baseUrl;
    }

    @Override
    public URL endpointUrlFor(String serviceEndpointPropertyName) {
        try {
            return new URL(execute(new HttpGet(baseUrl + PROPERTY_PATH + serviceEndpointPropertyName)).get("propertyValue").asText());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Map<String, String> allKnownExternalEndpoints() {
        try {
            ObjectNode result = execute(new HttpGet(baseUrl + PROPERTY_PATH + "all"));
            ArrayNode array = (ArrayNode) result.get("properties");
            Map<String, String> endPoints = new HashMap<>();
            for (int i = 0; i < array.size(); i++) {
                ObjectNode object = (ObjectNode) array.get(i);
                endPoints.put(object.get("propertyName").asText(), object.get("propertyValue").asText());
            }
            return endPoints;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}
