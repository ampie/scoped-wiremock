package com.sbg.bdd.wiremock.scoped.admin.endpointconfig;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.tomakehurst.wiremock.common.Json;
import com.sbg.bdd.wiremock.scoped.integration.EndpointConfig;
import com.sbg.bdd.wiremock.scoped.integration.HttpCommand;
import com.sbg.bdd.wiremock.scoped.integration.HttpCommandExecutor;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;


public class RemoteEndPointConfigRegistry {
    private static final Logger LOGGER = Logger.getLogger(RemoteEndPointConfigRegistry.class.getName());
    private final String baseUrl;
    private final String wireScope;
    public static final String ENDPOINT_CONFIG_PATH = "/MockableEndPoints/";
    private Set<EndpointConfig> endpointConfigs;

    public RemoteEndPointConfigRegistry(String baseUrl, String integrationScope) {
        this.baseUrl = baseUrl;
        if (integrationScope == null) {
            this.wireScope = "all";
        } else {
            this.wireScope = integrationScope;
        }
    }

    public Set<EndpointConfig> allKnownExternalEndpoints() {
        try {
            URL url = new URL(baseUrl + ENDPOINT_CONFIG_PATH);
            URLConnection connection = url.openConnection();
            connection.connect();
            HttpCommand command = new HttpCommand(new URL(baseUrl + ENDPOINT_CONFIG_PATH + wireScope), "GET", null);
            JsonNode result = Json.node(HttpCommandExecutor.INSTANCE.execute(command));
            ArrayNode array = (ArrayNode) result.get("configs");
            Set<EndpointConfig> endPoints = new TreeSet<>();
            for (int i = 0; i < array.size(); i++) {
                ObjectNode object = (ObjectNode) array.get(i);
                try {
                    endPoints.add(EndpointConfig.oneFromJson(object.toString()));
                } catch (IllegalArgumentException e) {
                    LOGGER.log(Level.WARNING,"Could not read EndPointConfig: " + object.toString(),e);
                }
            }
            this.endpointConfigs = endPoints;
            return this.endpointConfigs;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    //TODO optimise
    public EndpointConfig endpointUrlFor(String propertyName) {
        Set<EndpointConfig> endpointConfigs = allKnownExternalEndpoints();
        for (EndpointConfig endpointConfig : endpointConfigs) {
            if (endpointConfig.getPropertyName().equals(propertyName)) {
                return endpointConfig;
            }
        }
        return null;
    }
}
