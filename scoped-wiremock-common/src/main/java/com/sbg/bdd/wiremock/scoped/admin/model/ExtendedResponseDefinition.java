package com.sbg.bdd.wiremock.scoped.admin.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.http.*;

import java.util.List;

public class ExtendedResponseDefinition extends ResponseDefinition{
    private Boolean interceptFromSource;
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
                              @JsonProperty("proxyBaseUrl") String proxyBaseUrl,
                              @JsonProperty("fault") Fault fault,
                              @JsonProperty("transformers") List<String> transformers,
                              @JsonProperty("transformerParameters") Parameters transformerParameters,
                              @JsonProperty("fromConfiguredStub") Boolean wasConfigured,
                                      @JsonProperty("interceptFromSource") Boolean interceptFromSource) {
        super(status, statusMessage, body, jsonBody, base64Body, bodyFileName, headers, additionalProxyRequestHeaders, fixedDelayMilliseconds, delayDistribution, proxyBaseUrl, fault, transformers, transformerParameters, wasConfigured);
    }

    public ExtendedResponseDefinition(ResponseDefinition source) {
        super(source.getStatus(), source.getStatusMessage(), source.getBody(), null, source.getBase64Body(),
                source.getBodyFileName(), source.getHeaders(), source.getAdditionalProxyRequestHeaders(), source.getFixedDelayMilliseconds(),
                source.getDelayDistribution(), source.getProxyBaseUrl(), source.getFault(), source.getTransformers(), source.getTransformerParameters(), source.wasConfigured());
    }

    public Boolean getInterceptFromSource() {
        return interceptFromSource;
    }

    public void setInterceptFromSource(Boolean interceptFromSource) {
        this.interceptFromSource = interceptFromSource;
    }
}
