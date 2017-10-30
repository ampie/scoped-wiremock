package com.sbg.bdd.wiremock.scoped.admin.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import com.github.tomakehurst.wiremock.matching.RequestPattern;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;

import javax.xml.bind.annotation.XmlElement;
import java.util.UUID;

@JsonIgnoreProperties({"request", "response"})
@JsonPropertyOrder({"id", "name", "extendedRequest", "extendedResponse", "uuid"})
public class ExtendedStubMapping extends StubMapping {
        private ExtendedRequestPattern extendedRequest;
    @XmlElement(
            nillable = true
    )
    private ExtendedResponseDefinition extendedResponse;
    private ScopeLocalPriority localPriority;
    @XmlElement(
            nillable = true
    )
    private RecordingSpecification recordingSpecification;
    public ExtendedStubMapping(){}

    public ExtendedStubMapping(ExtendedRequestPattern requestPattern, ExtendedResponseDefinition response) {
        setExtendedRequest(requestPattern);
        setExtendedResponse(response);
    }

    public void setRecordingSpecification(RecordingSpecification recordingSpecification) {
        this.recordingSpecification = recordingSpecification;
    }

    public RecordingSpecification getRecordingSpecification() {
        return recordingSpecification;
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
