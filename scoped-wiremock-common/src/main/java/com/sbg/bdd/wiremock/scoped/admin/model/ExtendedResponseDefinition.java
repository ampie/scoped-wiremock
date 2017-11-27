package com.sbg.bdd.wiremock.scoped.admin.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.http.*;

import java.util.List;

public class ExtendedResponseDefinition extends ResponseDefinition {
    private boolean interceptFromSource = false;

    @JsonCreator
    public ExtendedResponseDefinition(@JsonProperty("status") int status,
                                      @JsonProperty("statusMessage") String statusMessage,
                                      @JsonProperty("body") String body,
                                      @JsonProperty("jsonBody") JsonNode jsonBody,
                                      @JsonProperty("base64Body") String base64Body,
                                      @JsonProperty("bodyFileName") String bodyFileName,
                                      @JsonProperty("headers") HttpHeaders headers,
                                      @JsonProperty("additionalProxyRequestHeaders") HttpHeaders additionalProxyRequestHeaders,
                                      @JsonProperty("fixedDelayMilliseconds") Integer fixedDelayMilliseconds,
                                      @JsonProperty("delayDistribution") DelayDistribution delayDistribution,
                                      @JsonProperty("chunkedDribbleDelay") ChunkedDribbleDelay chunkedDribbleDelay,
                                      @JsonProperty("proxyBaseUrl") String proxyBaseUrl,
                                      @JsonProperty("fault") Fault fault,
                                      @JsonProperty("transformers") List<String> transformers,
                                      @JsonProperty("transformerParameters") Parameters transformerParameters,
                                      @JsonProperty("fromConfiguredStub") Boolean wasConfigured,
                                      @JsonProperty("interceptFromSource") Boolean interceptFromSource) {
        super(status, statusMessage, body, jsonBody, base64Body, bodyFileName, headers, additionalProxyRequestHeaders, fixedDelayMilliseconds, delayDistribution, chunkedDribbleDelay, proxyBaseUrl, fault, transformers, transformerParameters, wasConfigured);
        this.interceptFromSource = interceptFromSource;
    }

    public ExtendedResponseDefinition(ResponseDefinition source) {
        super(source.getStatus(), source.getStatusMessage(), source.getBody(), null, source.getBase64Body(),
                source.getBodyFileName(), source.getHeaders(), source.getAdditionalProxyRequestHeaders(), source.getFixedDelayMilliseconds(),
                source.getDelayDistribution(), source.getChunkedDribbleDelay(), source.getProxyBaseUrl(), source.getFault(), source.getTransformers(), source.getTransformerParameters(), source.wasConfigured());
    }

    public boolean isInterceptFromSource() {
        return interceptFromSource;
    }

    public void setInterceptFromSource(boolean interceptFromSource) {
        this.interceptFromSource = interceptFromSource;
    }
}
