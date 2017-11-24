package com.sbg.bdd.wiremock.scoped.admin.endpointconfig;


import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.tomakehurst.wiremock.common.Json;
import com.sbg.bdd.wiremock.scoped.integration.EndpointConfig;
import com.sbg.bdd.wiremock.scoped.integration.HttpCommand;
import com.sbg.bdd.wiremock.scoped.integration.HttpCommandExecutor;

import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;


public class RemoteEndpointConfigRegistry {
    private static final Logger LOGGER = Logger.getLogger(RemoteEndpointConfigRegistry.class.getName());
    private String baseUrl;
    private String integrationScope;
    private Set<EndpointConfig> endpointConfigs;
    private Map<String, EndpointConfig> endpointConfigsByPropertyName;
    private Map<String, List<EndpointConfig>> endpointConfigsByCategory;

    public RemoteEndpointConfigRegistry(String baseUrl, String integrationScope) {
        this.baseUrl = baseUrl;
        this.integrationScope = integrationScope;
    }

    public RemoteEndpointConfigRegistry() {

    }

    public EndpointConfig endpointConfigFor(String propertyName) {
        allKnownExternalEndpoints();
        return endpointConfigsByPropertyName.get(propertyName);
    }

    public Set<EndpointConfig> allKnownExternalEndpoints() {
        if (endpointConfigs == null) {
            this.endpointConfigs = loadEndpoints();
            populateMaps();

        }
        return this.endpointConfigs;
    }
    public List<EndpointConfig> endpointConfigsInCategory(String cateogry) {
        allKnownExternalEndpoints();
        return endpointConfigsByCategory.get(cateogry);
    }

    private void populateMaps() {
        endpointConfigsByPropertyName=new HashMap<>();
        endpointConfigsByCategory= new HashMap<>();
        for (EndpointConfig endpointConfig : endpointConfigs) {
            endpointConfigsByPropertyName.put(endpointConfig.getPropertyName(),endpointConfig);
            for (String s : endpointConfig.getCategories()) {
                List<EndpointConfig> category = endpointConfigsByCategory.get(s);
                if(category==null){
                    endpointConfigsByCategory.put(s, category=new ArrayList<>());
                }
                category.add(endpointConfig);
            }
        }
    }


    private Set<EndpointConfig> loadEndpoints() {
        try {
            return retrieveInScopeEndpointConfigsRecursively(this.baseUrl);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Set<EndpointConfig> retrieveInScopeEndpointConfigsRecursively(String baseUrl) throws IOException {
        Set<EndpointConfig> endPoints = retrieveEndpointsFrom(baseUrl);
        return addTransitiveInScopeEndpoints(endPoints);
    }

    private Set<EndpointConfig> addTransitiveInScopeEndpoints(Set<EndpointConfig> endPoints) throws IOException {
        Set<EndpointConfig> results = new TreeSet<>();
        if (!isLocalScope()) {
            for (EndpointConfig endPoint : endPoints) {
                if (endPoint.getScopes().contains(integrationScope)) {
                    results.addAll(retrieveEndpointsFrom(endPoint.getUrl().toExternalForm()));
                }
            }
        }
        results.addAll(endPoints);
        return results;
    }

    private Set<EndpointConfig> retrieveEndpointsFrom(String baseUrl) throws IOException {
        Set<EndpointConfig> endPoints = new TreeSet<>();
        HttpCommand command = new HttpCommand(new URL(baseUrl + EndpointConfig.ENDPOINT_CONFIG_PATH + "all"), "GET", null);
        ArrayNode array = (ArrayNode) Json.node(HttpCommandExecutor.INSTANCE.execute(command));
        for (int i = 0; i < array.size(); i++) {
            ObjectNode object = (ObjectNode) array.get(i);
            try {
                endPoints.add(EndpointConfig.oneFromJson(object.toString()));
            } catch (IllegalArgumentException e) {
                LOGGER.log(Level.WARNING, "Could not read EndPointConfig: " + object.toString(), e);
            }
        }
        return endPoints;
    }

    private boolean isLocalScope() {
        return integrationScope.equals(EndpointConfig.LOCAL_INTEGRATION_SCOPE);
    }

}
