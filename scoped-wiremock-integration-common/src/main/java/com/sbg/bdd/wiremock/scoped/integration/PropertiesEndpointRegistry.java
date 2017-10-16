package com.sbg.bdd.wiremock.scoped.integration;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;

public class PropertiesEndpointRegistry implements EndpointRegistry {
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

}
