package com.sbg.bdd.wiremock.scoped.admin.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import com.github.tomakehurst.wiremock.matching.RequestPattern;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;

import java.util.UUID;

@JsonIgnoreProperties({"request", "response"})
@JsonPropertyOrder({"id", "name", "extendedRequest", "extendedResponse", "uuid"})
public class ExtendedStubMapping extends StubMapping {
    private ExtendedRequestPattern extendedRequest;
    private ExtendedResponseDefinition extendedResponse;
    private ScopeLocalPriority localPriority;

    public ExtendedStubMapping(){}

    public ExtendedStubMapping(ExtendedRequestPattern requestPattern, ExtendedResponseDefinition response) {
        setExtendedRequest(requestPattern);
        setExtendedResponse(response);
    }

    public ExtendedRequestPattern getExtendedRequest() {
        return extendedRequest;
    }

    public void setExtendedRequest(ExtendedRequestPattern extendedRequest) {
        this.extendedRequest = extendedRequest;
        super.setRequest(extendedRequest);
    }

    @Override
    public ExtendedRequestPattern getRequest() {
        return getExtendedRequest();
    }

    @Override
    public void setRequest(RequestPattern request) {
        this.extendedRequest = (ExtendedRequestPattern) request;
        super.setRequest(request);
    }

    public ExtendedResponseDefinition getExtendedResponse() {
        return extendedResponse;
    }

    public void setExtendedResponse(ExtendedResponseDefinition extendedResponse) {
        this.extendedResponse = extendedResponse;
        super.setResponse(extendedResponse);
    }

    @Override
    public ExtendedResponseDefinition getResponse() {
        return getExtendedResponse();
    }

    @Override
    public void setResponse(ResponseDefinition response) {
        this.extendedResponse = (ExtendedResponseDefinition) response;
        super.setResponse(response);
    }

    public void setLocalPriority(ScopeLocalPriority localPriority) {
        this.localPriority = localPriority;
    }

    public ScopeLocalPriority getLocalPriority() {
        return localPriority;
    }
}
