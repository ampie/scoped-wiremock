package com.sbg.bdd.wiremock.scoped.client.builders;

import com.github.tomakehurst.wiremock.client.BasicCredentials;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.ScenarioMappingBuilder;
import com.github.tomakehurst.wiremock.matching.ContentPattern;
import com.github.tomakehurst.wiremock.matching.StringValuePattern;
import com.sbg.bdd.wiremock.scoped.admin.model.*;
import com.sbg.bdd.wiremock.scoped.client.WireMockContext;

import java.util.*;

public class ExtendedMappingBuilder<T extends ExtendedMappingBuilder> implements MappingBuilder {
    //Extended
    private ExtendedRequestPatternBuilder requestPatternBuilder;
    private ExtendedResponseDefinitionBuilder responseDefinitionBuilder;
    private RecordingSpecification recordingSpecification;
    private ResponseStrategy responseStrategy;
    //Standard
    private String name;
    private UUID id = UUID.randomUUID();
    private Boolean persistent;
    private ScopeLocalPriority localPriority;

    public ExtendedMappingBuilder(ExtendedRequestPatternBuilder requestPatternBuilder, ExtendedResponseDefinitionBuilder responseDefinitionBuilder, RecordingSpecification recordingSpecification) {
        this(requestPatternBuilder);
        if (responseDefinitionBuilder != null) {
            this.responseDefinitionBuilder = new ExtendedResponseDefinitionBuilder(responseDefinitionBuilder);
        }
        if (recordingSpecification != null) {
            this.recordingSpecification = new RecordingSpecification(recordingSpecification);
        }
    }

    public ExtendedMappingBuilder(ExtendedRequestPatternBuilder requestPatternBuilder) {
        this.requestPatternBuilder = new ExtendedRequestPatternBuilder(requestPatternBuilder);
    }


    public T withRequestBody(ContentPattern bodyPattern) {
        this.requestPatternBuilder.withRequestBody(bodyPattern);
        return (T) this;
    }


    public ScenarioMappingBuilder inScenario(String scenarioName) {
        throw new IllegalStateException("Scenarios not supported");
    }


    public T withId(UUID id) {
        this.id = id;
        return (T) this;
    }


    public T withName(String name) {
        this.name = name;
        return (T) this;
    }


    public T persistent() {
        this.persistent = true;
        return (T) this;
    }


    public T withBasicAuth(String username, String password) {
        this.requestPatternBuilder.withBasicAuth(new BasicCredentials(username, password));
        return (T) this;
    }


    public T withCookie(String name, StringValuePattern cookieValuePattern) {
        this.requestPatternBuilder.withCookie(name, cookieValuePattern);
        return (T) this;
    }


    public <P> T withPostServeAction(String extensionName, P parameters) {
        throw new IllegalStateException("PostServeActions not supported");
    }


    public T withHeader(String key, StringValuePattern headerPattern) {
        this.requestPatternBuilder.withHeader(key, headerPattern);
        return (T) this;
    }


    public T withQueryParam(String key, StringValuePattern queryParamPattern) {
        this.requestPatternBuilder.withQueryParam(key, queryParamPattern);
        return (T) this;
    }


    public T to(String urlInfo) {
        this.requestPatternBuilder.to(urlInfo);
        return (T) this;
    }

    public T recordingResponsesTo(String dir) {
        getRecordingSpecification().recordingResponsesTo(dir);
        return (T) this;
    }


    public T will(ResponseStrategy responseStrategy) {
        return to(responseStrategy);
    }


    public T to(ResponseStrategy responseStrategy) {
        this.responseStrategy = responseStrategy;
        return (T) this;
    }


    public T atPriority(Integer priority) {
        if (priority != null)
            if (priority >= ScopeLocalPriority.values().length) {
                throw new IllegalArgumentException("ExtendedMappings only support the priority values provided in the " + ScopeLocalPriority.class.getName() + " class");
            } else {
                this.localPriority = ScopeLocalPriority.values()[priority];
            }
        return (T) this;
    }

    public ExtendedRequestPatternBuilder getRequestPatternBuilder() {
        return requestPatternBuilder;
    }

    public ExtendedResponseDefinitionBuilder getResponseDefinitionBuilder() {
        return responseDefinitionBuilder;
    }

    public String getUrlInfo() {
        return this.requestPatternBuilder.getUrlInfo();
    }

    public T willReturn(ResponseDefinitionBuilder responseDefBuilder) {
        if (responseDefBuilder instanceof ExtendedResponseDefinitionBuilder) {
            this.responseDefinitionBuilder = (ExtendedResponseDefinitionBuilder) responseDefBuilder;
        } else if (responseDefBuilder != null) {
            this.responseDefinitionBuilder = new ExtendedResponseDefinitionBuilder(responseDefBuilder);
        }
        return (T) this;
    }


    public ExtendedStubMapping build() {
        ExtendedRequestPattern requestPattern = requestPatternBuilder.build();
        ExtendedResponseDefinition response = responseDefinitionBuilder==null?null:responseDefinitionBuilder.build();
        ExtendedStubMapping mapping = new ExtendedStubMapping(requestPattern, response);
        mapping.setLocalPriority(localPriority);
        mapping.setUuid(id);
        mapping.setName(name);
        mapping.setPersistent(persistent);
        mapping.setRecordingSpecification(recordingSpecification);
        return mapping;
    }


    public RecordingSpecification getRecordingSpecification() {
        if (recordingSpecification == null) {
            recordingSpecification = new RecordingSpecification();
        }
        return recordingSpecification;
    }

    public void applyTo(WireMockContext verificationContext) {
        prepareForBuildWithin(verificationContext);
        registerTo(verificationContext);
    }

    private void registerTo(WireMockContext verificationContext) {
        verificationContext.register(this);
    }

    private void prepareForBuildWithin(WireMockContext verificationContext) {
        getRequestPatternBuilder().setCorrelationPath(verificationContext.getCorrelationPath());
        if (responseDefinitionBuilder == null) {
            try {
                responseDefinitionBuilder = responseStrategy.applyTo(this, verificationContext);
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        //This may not have been created by one of the known ResponseBodyStrategies, so let's just give it a default priority and assume the user wants this as a priority
        if (localPriority == null && verificationContext != null) {
            atPriority(ScopeLocalPriority.BODY_KNOWN);
        }
    }

    public void atPriority(ScopeLocalPriority scopeLocalPriority) {
        this.localPriority = scopeLocalPriority;
    }
}
