package com.sbg.bdd.wiremock.scoped.admin.model;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import com.github.tomakehurst.wiremock.matching.RequestPattern;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;

@JsonPropertyOrder({ "id", "name", "request", "newRequest", "response", "uuid" })
public class ExtendedStubMapping extends StubMapping{
    private ExtendedRequestPattern request;
    private ExtendedResponseDefinition response;

    public ExtendedStubMapping() {

    }
    public ExtendedStubMapping(ExtendedRequestPattern requestPattern, ExtendedResponseDefinition response) {
        setRequest(requestPattern);
        setResponse(response);
    }

    @Override
    public ExtendedRequestPattern getRequest() {
        return this.request;
    }

    @Override
    public void setRequest(RequestPattern request) {
        this.request= (ExtendedRequestPattern) request;
        super.setRequest(request);
    }

    @Override
    public ExtendedResponseDefinition getResponse() {
        return response;
    }

    @Override
    public void setResponse(ResponseDefinition response) {
        this.response= (ExtendedResponseDefinition) response;
        super.setResponse(response);
    }
}
