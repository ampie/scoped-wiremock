package com.sbg.bdd.wiremock.scoped.integration;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class PropertiesEndpointRegistry implements EndPointRegistry {
    private Properties properties = new Properties();

    public PropertiesEndpointRegistry(Properties properties) {
        this.properties = properties;
    }

    public PropertiesEndpointRegistry(File file) {
        this(loadFrom(file));
    }

    private static Properties loadFrom(File file) {
        Properties props = new Properties();
        try {
            props.load(new FileInputStream(file));
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
        return props;
    }

    @Override
    public URL endpointUrlFor(String serviceEndpointPropertyName) {
        try {
            return new URL(properties.getProperty(serviceEndpointPropertyName));
        } catch (MalformedURLException e) {
            return null;
        }
    }

    @Override
    public Map<String, URL> allKnownExternalEndpoints() {
        Map<String, URL> result = new HashMap<>();
        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
            try {
                URL url = new URL(entry.getValue().toString());
                if (url.getProtocol().startsWith("http")) {
                    result.put(entry.getKey().toString(), url);
                }
            } catch (MalformedURLException e) {
            }
        }
        return result;
    }
}
