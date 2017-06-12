package com.sbg.bdd.wiremock.scoped.integration;

import java.net.MalformedURLException;
import java.net.URL;

public class URLHelper {
    public static String identifier(URL originalUrl, String httpMethod) {
        return originalUrl.getProtocol() + ":" + httpMethod + "://" + originalUrl.getAuthority() + originalUrl.getPath();
    }

    public static String identifier(URL originalUrl) {
        return originalUrl.getProtocol() + "://" + originalUrl.getAuthority() + originalUrl.getPath();
    }

    public static URL calculateOriginalUrl(URL currentUrl, URL originalHost) {
        try {
            URL url = replaceBaseUrl(currentUrl, originalHost);
            return new URL(url.getProtocol() + "://" + url.getAuthority() + url.getPath());
        } catch (MalformedURLException e) {
            throw new IllegalStateException(e);
        }
    }

    public static URL replaceBaseUrl(URL originalUrl, URL newBaseUrl) {
        try {
            String baseUrl = newBaseUrl.toExternalForm();
            if (baseUrl.endsWith("/")) {
                baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
            }
            return new URL(baseUrl + originalUrl.getPath() + (originalUrl.getQuery() != null ? "?" + originalUrl.getQuery() : ""));
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static URL hostOnly(URL url) {
        try {
            return new URL(url.getProtocol() + "://" + url.getAuthority());
        } catch (MalformedURLException e) {
            throw new IllegalStateException(e);
        }
    }
}
