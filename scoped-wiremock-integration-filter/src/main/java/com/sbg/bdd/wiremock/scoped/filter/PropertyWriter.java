package com.sbg.bdd.wiremock.scoped.filter;


import com.sbg.bdd.wiremock.scoped.integration.EndPointRegistry;

import javax.servlet.ServletOutputStream;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;

public class PropertyWriter {
    private EndPointRegistry endpointRegistry;


    public PropertyWriter(EndPointRegistry endpointRegistry) {
        this.endpointRegistry = endpointRegistry;
    }

    public boolean maybeWriteOneOrAllProperties(ServletOutputStream outputStream, String propertyName, boolean resolveIp) throws IOException {
        if (propertyName.equals("all")) {
            outputStream.print("{\"properties\":[");
            Set<String> soapEndpointProperties = KnownEndpointRegistry.getInstance().getSoapEndpointProperties();
            Set<String> restEndpointProperties = KnownEndpointRegistry.getInstance().getRestEndpointProperties();
            if (soapEndpointProperties.size() > 0) {
                writeProperties(outputStream, soapEndpointProperties, resolveIp);
                if (restEndpointProperties.size() > 0) {
                    outputStream.print(",");
                    writeProperties(outputStream, restEndpointProperties, resolveIp);
                }
            } else {
                writeProperties(outputStream, restEndpointProperties, resolveIp);
            }
            outputStream.print("]}");
            return true;
        } else {
            String propertyNameValue = extractProperty(propertyName, resolveIp);
            if (propertyNameValue != null) {
                outputStream.print(propertyNameValue);
            }
            return propertyNameValue != null;
        }
    }

    private void writeProperties(ServletOutputStream outputStream, Set<String> soapEndpointProperties, boolean resolveIp) {
        Iterator<String> iterator = soapEndpointProperties.iterator();
        while (iterator.hasNext()) {
            try {
                String propertyNameValue = extractProperty(iterator.next(), resolveIp);
                if (propertyNameValue != null) {
                    outputStream.print(propertyNameValue);
                    if (iterator.hasNext()) {
                        outputStream.print(",");
                    }
                }
            } catch (IOException e) {
                InboundCorrelationPathFilter.LOGGER.log(Level.WARNING, e.getMessage(), e);
            }
        }
    }

    private String extractProperty(String propertyName, boolean resolveIp) throws MalformedURLException, UnknownHostException {
        URL url = endpointRegistry.endpointUrlFor(propertyName);
        if(url == null){
            String possibleValue = KnownEndpointRegistry.getInstance().getTransitiveEndpoint(propertyName);
            if(possibleValue!=null) {
                url = new URL(possibleValue);
            }
        }
        if (url != null) {
            if (resolveIp) {
                String ipAddress = Inet4Address.getByName(url.getHost()).getHostAddress();
                url = new URL(url.getProtocol(), ipAddress, url.getPort(), url.getFile());
            }
            String property = url.toExternalForm();
            return "{\"propertyName\":\"" + propertyName + "\",\"propertyValue\":\"" + property + "\"}";
        }
        return null;
    }
}
