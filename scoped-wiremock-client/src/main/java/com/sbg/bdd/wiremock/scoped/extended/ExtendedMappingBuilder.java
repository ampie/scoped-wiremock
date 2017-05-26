package com.sbg.bdd.wiremock.scoped.extended;

import com.github.tomakehurst.wiremock.client.BasicCredentials;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.ScenarioMappingBuilder;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import com.github.tomakehurst.wiremock.matching.RequestPattern;
import com.github.tomakehurst.wiremock.matching.StringValuePattern;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import com.sbg.bdd.wiremock.scoped.WireMockContext;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ExtendedMappingBuilder<T extends ExtendedMappingBuilder> implements MappingBuilder {
    private ExtendedRequestPatternBuilder requestPatternBuilder;
    private ExtendedResponseDefinitionBuilder responseDefinitionBuilder;
    private RecordingSpecification recordingSpecification;
    private String name;
    private UUID id;
    private Integer priority;
    private List<ExtendedMappingBuilder> children = new ArrayList<>();
    private Boolean persistent;
    private ResponseStrategy responseStrategy;

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

    
    public T withRequestBody(StringValuePattern bodyPattern) {
        this.requestPatternBuilder.withRequestBody(bodyPattern);
        return (T)this;
    }

    
    public ScenarioMappingBuilder inScenario(String scenarioName) {
        throw new IllegalStateException("Scenarios not supported");
    }

    
    public MappingBuilder withId(UUID id) {
        this.id = id;
        return (T)this;
    }

    
    public MappingBuilder withName(String name) {
        this.name = name;
        return (T)this;
    }

    
    public MappingBuilder persistent() {
        this.persistent = true;
        return (T)this;
    }

    
    public MappingBuilder withBasicAuth(String username, String password) {
        this.requestPatternBuilder.withBasicAuth(new BasicCredentials(username, password));
        return (T)this;
    }

    
    public MappingBuilder withCookie(String name, StringValuePattern cookieValuePattern) {
        this.requestPatternBuilder.withCookie(name, cookieValuePattern);
        return (T)this;
    }

    
    public <P> MappingBuilder withPostServeAction(String extensionName, P parameters) {
        throw new IllegalStateException("PostServeActions not supported");
    }

    
    public T withHeader(String key, StringValuePattern headerPattern) {
        this.requestPatternBuilder.withHeader(key, headerPattern);
        return (T)this;
    }

    
    public MappingBuilder withQueryParam(String key, StringValuePattern queryParamPattern) {
        this.requestPatternBuilder.withQueryParam(key, queryParamPattern);
        return (T)this;
    }

    
    public T to(String urlInfo) {
        this.requestPatternBuilder.to(urlInfo);
        return (T)this;
    }

    
    public T will(ResponseStrategy responseStrategy) {
        return to(responseStrategy);
    }

    
    public T to(ResponseStrategy responseStrategy) {
        this.responseStrategy = responseStrategy;
        return (T)this;
    }


    private void createChildren(WireMockContext verificationContext) {
        if (requestPatternBuilder.isToAllKnownExternalServices()) {
            Map<String, String> allEndpoints = verificationContext.allKnownExternalEndpoints();
            for (Map.Entry<String, String> entry : allEndpoints.entrySet()) {
                String urlOfSource = entry.getValue();
                try {
                    URL url = new URL(urlOfSource);
                    ExtendedMappingBuilder newBuilder = new ExtendedMappingBuilder(requestPatternBuilder, responseDefinitionBuilder, recordingSpecification);
                    newBuilder.to(url.getPath() + ".*");
                    if (responseDefinitionBuilder != null && responseDefinitionBuilder.interceptFromSource()) {
                        String proxiedBaseUrl = url.getProtocol() + "://" + url.getAuthority();
                        newBuilder.getResponseDefinitionBuilder().proxiedFrom(proxiedBaseUrl);
                    }
                    newBuilder.atPriority(getPriority());
                    newBuilder.getRequestPatternBuilder().toAnyKnownExternalService(false);
                    newBuilder.getRequestPatternBuilder().prepareForBuild(verificationContext);
                    addChildBuilder(newBuilder);
                } catch (MalformedURLException e) {
                    System.out.println("Could not load knownExternalEndpoint '" + entry.getKey() + "'='" + entry.getValue() + "'");
                }

            }
        } else if (responseDefinitionBuilder != null && responseDefinitionBuilder.interceptFromSource()) {
            URL url = verificationContext.endpointUrlFor(getUrlInfo());
            String proxiedBaseUrl = url.getProtocol() + "://" + url.getAuthority();
            responseDefinitionBuilder.proxiedFrom(proxiedBaseUrl);
        }

    }

    
    public T atPriority(Integer priority) {
        this.priority = priority;
        return (T)this;
    }

    public Integer getPriority() {
        return priority;
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

    public void addChildBuilder(ExtendedMappingBuilder newBuilder) {
        this.children.add((ExtendedMappingBuilder) newBuilder);
    }

    public T willReturn(ResponseDefinitionBuilder responseDefBuilder) {
        if (responseDefBuilder instanceof ExtendedResponseDefinitionBuilder) {
            this.responseDefinitionBuilder = (ExtendedResponseDefinitionBuilder) responseDefBuilder;
        } else if (responseDefBuilder != null) {
            this.responseDefinitionBuilder = new ExtendedResponseDefinitionBuilder(responseDefBuilder);
        }
        return (T)this;
    }

    
    public StubMapping build() {
        RequestPattern requestPattern = requestPatternBuilder.build();
        ResponseDefinition response = responseDefinitionBuilder.build();
        StubMapping mapping = new StubMapping(requestPattern, response);
        mapping.setPriority(priority);
        mapping.setUuid(id);
        mapping.setName(name);
        mapping.setPersistent(persistent);
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
        if (children.size() > 0) {
            for (ExtendedMappingBuilder child : children) {
                verificationContext.register(child);
            }
        } else {
            verificationContext.register(this);
        }
    }

    private void prepareForBuildWithin(WireMockContext verificationContext) {
        if (responseDefinitionBuilder == null) {
            try {
                responseDefinitionBuilder = responseStrategy.applyTo(this, verificationContext);
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        getRequestPatternBuilder().prepareForBuild(verificationContext);
        //This may not have been created by one of the known ResponseBodyStrategies, so let's just give it a default priority and assume the user wants this as a priority
        if (getPriority() == null && verificationContext != null) {
            atPriority(verificationContext.calculatePriority(1));
        }
        createChildren(verificationContext);
    }

}
