package com.sbg.bdd.wiremock.scoped.admin.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.tomakehurst.wiremock.client.BasicCredentials;
import com.github.tomakehurst.wiremock.http.RequestMethod;
import com.github.tomakehurst.wiremock.matching.*;

import java.net.URL;
import java.util.List;
import java.util.Map;

public class ExtendedRequestPattern extends RequestPattern {
    private String urlInfo;
    private String pathSuffix;
    private boolean urlIsPattern = false;
    private boolean toAllKnownExternalServices = false;
    private String endpointCategory;


    @JsonCreator
    public ExtendedRequestPattern(@JsonProperty("url") String url,
                                  @JsonProperty("urlPattern") String urlPattern,
                                  @JsonProperty("urlPath") String urlPath,
                                  @JsonProperty("urlPathPattern") String urlPathPattern,
                                  @JsonProperty("method") RequestMethod method,
                                  @JsonProperty("headers") Map<String, MultiValuePattern> headers,
                                  @JsonProperty("queryParameters") Map<String, MultiValuePattern> queryParams,
                                  @JsonProperty("cookies") Map<String, StringValuePattern> cookies,
                                  @JsonProperty("basicAuth") BasicCredentials basicAuthCredentials,
                                  @JsonProperty("bodyPatterns") List<StringValuePattern> bodyPatterns,
                                  @JsonProperty("customMatcher") CustomMatcherDefinition customMatcherDefinition,
                                  @JsonProperty("urlInfo") String urlInfo,
                                  @JsonProperty("pathSuffix") String pathSuffix,
                                  @JsonProperty("urlIsPattern") boolean urlIsPattern,
                                  @JsonProperty("toAllKnownExternalServices") boolean toAllKnownExternalServices,
                                  @JsonProperty("endpointCategory") String endpointCategory
    ) {

        super(
                UrlPattern.fromOneOf(url, urlPattern, urlPath, urlPathPattern),
                method,
                headers,
                queryParams,
                cookies,
                basicAuthCredentials,
                bodyPatterns,
                customMatcherDefinition
        );
        this.urlInfo = urlInfo;
        this.pathSuffix = pathSuffix;
        this.urlIsPattern = urlIsPattern;
        this.toAllKnownExternalServices = toAllKnownExternalServices;
        this.endpointCategory = endpointCategory;
    }

    public ExtendedRequestPattern(RequestPattern source) {
        super(
                source.getUrlMatcher(),
                source.getMethod(),
                source.getHeaders(),
                source.getQueryParameters(),
                source.getCookies(),
                source.getBasicAuthCredentials(),
                source.getBodyPatterns(),
                source.getCustomMatcher()
        );
        setUrlInfo(source.getUrlMatcher().getExpected());
    }

    public String getUrlInfo() {
        return urlInfo;
    }

    public String getPathSuffix() {
        return pathSuffix;
    }

    public boolean isUrlIsPattern() {
        return urlIsPattern;
    }

    public boolean isToAllKnownExternalServices() {
        return toAllKnownExternalServices;
    }

    public String getEndpointCategory() {
        return endpointCategory;
    }

    public void setUrlInfo(String urlInfo) {
        this.urlInfo = urlInfo;
    }

    public void setPathSuffix(String pathSuffix) {
        this.pathSuffix = pathSuffix;
    }

    public void setUrlIsPattern(boolean urlIsPattern) {
        this.urlIsPattern = urlIsPattern;
    }

    public void setToAllKnownExternalServices(boolean toAllKnownExternalServices) {
        this.toAllKnownExternalServices = toAllKnownExternalServices;
    }

    public void setEndpointCategory(String endpointCategory) {
        this.endpointCategory = endpointCategory;
    }
}
