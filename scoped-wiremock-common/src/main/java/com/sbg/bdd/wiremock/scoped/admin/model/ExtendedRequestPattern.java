package com.sbg.bdd.wiremock.scoped.admin.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.tomakehurst.wiremock.client.BasicCredentials;
import com.github.tomakehurst.wiremock.http.RequestMethod;
import com.github.tomakehurst.wiremock.matching.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExtendedRequestPattern extends RequestPattern {
    private String correlationPath;
    private String urlInfo;
    private String pathSuffix;
    private boolean urlIsPattern = false;
    private boolean toAllKnownExternalServices = false;
    private String endpointCategory;


    @JsonCreator
    public ExtendedRequestPattern(@JsonProperty("correlationPath") String correlationPath,
                                  @JsonProperty("url") String url,
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
        this.correlationPath = correlationPath;
        this.urlInfo = urlInfo;
        this.pathSuffix = pathSuffix;
        this.urlIsPattern = urlIsPattern;
        this.toAllKnownExternalServices = toAllKnownExternalServices;
        this.endpointCategory = endpointCategory;
    }

    public ExtendedRequestPattern(String correlationPath, RequestPattern source) {
        super(
                source.getUrlMatcher(),
                source.getMethod(),
                source.getHeaders() == null ? new HashMap<String, MultiValuePattern>() : source.getHeaders(),//because we always have headers and need to manipulate them outside of a RequestPatternBuilder
                source.getQueryParameters(),
                source.getCookies(),
                source.getBasicAuthCredentials(),
                source.getBodyPatterns(),
                source.getCustomMatcher()
        );
        this.correlationPath = correlationPath;
        UrlPattern urlMatcher = source.getUrlMatcher();
        if (source instanceof ExtendedRequestPattern) {
            ExtendedRequestPattern extendedRequestPattern = (ExtendedRequestPattern) source;
            this.urlInfo = extendedRequestPattern.getUrlInfo();
            this.pathSuffix = extendedRequestPattern.getPathSuffix();
            this.urlIsPattern=extendedRequestPattern.isUrlIsPattern();
            this.toAllKnownExternalServices=extendedRequestPattern.isToAllKnownExternalServices();
            this.endpointCategory=extendedRequestPattern.getEndpointCategory();
        } else {
            if (urlMatcher != null && (urlMatcher.getPattern() instanceof EqualToPattern || urlMatcher.getPattern() instanceof RegexPattern)) {
                //try to reuse it where we can
                setUrlInfo(urlMatcher.getExpected());
            }
        }
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

    public String getCorrelationPath() {
        return correlationPath;
    }

}
