package com.sbg.bdd.wiremock.scoped;

import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.Admin;
import com.github.tomakehurst.wiremock.matching.MultiValuePattern;
import com.github.tomakehurst.wiremock.matching.RequestPattern;
import com.github.tomakehurst.wiremock.matching.StringValuePattern;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import com.sbg.bdd.resource.ResourceContainer;
import com.sbg.bdd.wiremock.scoped.admin.ScopedAdmin;
import com.sbg.bdd.wiremock.scoped.admin.model.*;
import com.sbg.bdd.wiremock.scoped.common.CanStartAndStop;
import com.sbg.bdd.wiremock.scoped.common.HasBaseUrl;
import com.sbg.bdd.wiremock.scoped.integration.HeaderName;

import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.sbg.bdd.wiremock.scoped.common.Reflection.setValue;


public abstract class ScopedWireMock extends WireMock implements HasBaseUrl {

    protected static final int DEFAULT_PORT = 8080;
    protected static final String DEFAULT_HOST = "localhost";
    protected ScopedAdmin admin;

    public ScopedWireMock(ScopedAdmin admin) {
        super((Admin) admin);
        this.admin = admin;
    }

    public void stopServerIfRunningLocally() {
        if (admin instanceof CanStartAndStop) {
            ((CanStartAndStop) admin).stop();
        }
    }
    public ResourceContainer getResourceRoot(String name){
        return admin.getResourceRoot(name);
    }

    public int port() {
        return admin instanceof HasBaseUrl ? ((HasBaseUrl) admin).port() : DEFAULT_PORT;
    }

    public String host() {
        return admin instanceof HasBaseUrl ? ((HasBaseUrl) admin).host() : DEFAULT_HOST;
    }

    @Override
    public String baseUrl() {
        return "http://" + host() + ":" + port();
    }

    //Scope management
    public GlobalCorrelationState startNewGlobalScope(String testRunName, URL wireMockPublicUrl, URL baseUrlOfServiceUnderTest, String integrationScope, Map<String, Object> payload) {
        GlobalCorrelationState state = new GlobalCorrelationState(testRunName, wireMockPublicUrl, baseUrlOfServiceUnderTest, integrationScope);
        state.getPayload().putAll(payload);
        return startNewGlobalScope(state);
    }

    public GlobalCorrelationState startNewGlobalScope(GlobalCorrelationState state) {
        return admin.startNewGlobalScope(state);
    }

    public GlobalCorrelationState stopGlobalScope(String testRunName, URL wireMockPublicUrl, int sequenceNumber, Map<String, Object> payload) {
        GlobalCorrelationState state = new GlobalCorrelationState(testRunName, wireMockPublicUrl, sequenceNumber);
        state.getPayload().putAll(payload);
        return stopGlobalScope(state);
    }

    public GlobalCorrelationState stopGlobalScope(GlobalCorrelationState state) {
        return admin.stopGlobalScope(state);
    }

    public CorrelationState startNestedScope(String parentCorrelationPath, String name, Map<String, Object> payload) {
        return admin.startNestedScope(new InitialScopeState(parentCorrelationPath,name, payload));
    }

    public List<String> stopNestedScope(String knownScopePath, Map<String, Object> map) {
        return admin.stopNestedScope(new CorrelationState(knownScopePath, map));
    }

    public CorrelationState getCorrelatedScope(String scopePath) {
        return admin.getCorrelatedScope(scopePath);
    }

    public void syncCorrelatedScope(CorrelationState nestedScope) {
        admin.syncCorrelatedScope(nestedScope);
    }

    public List<StubMapping> getMappingsInScope(String scopePath) {
        return admin.getMappingsInScope(scopePath);
    }
    //User scope mapping
    public CorrelationState startUserScope(String parentCorrelationPath, String userName, Map<String,Object> payload) {
        return admin.startUserScope(new InitialScopeState(parentCorrelationPath,userName, payload));
    }
    //Step management
    public void startStep(String correlationPath, String stepName, Map<String, Object> payload) {
        admin.startStep(new CorrelationState(correlationPath, stepName, payload));
    }

    public List<RecordedExchange> findExchangesAgainstStep(String scopePath, String stepName) {
        return admin.findExchangesAgainstStep(scopePath, stepName);
    }

    public void stopStep(String scopePath, String stepName, Map<String, Object> payload) {
        admin.stopStep(new CorrelationState(scopePath, stepName, payload));
    }

    //Recording management
    public void saveRecordingsForRequestPattern(StringValuePattern scopePath, ExtendedRequestPattern pattern, ResourceContainer recordingDirectory) {
        addScopePathHeader(scopePath, pattern);
        admin.saveRecordingsForRequestPattern(pattern, recordingDirectory);
    }

    public void serveRecordedMappingsAt(ResourceContainer directoryRecordedTo, ExtendedRequestPattern requestPattern, int priority) {
        admin.serveRecordedMappingsAt(directoryRecordedTo, requestPattern, priority);
    }

    //Mappings management
    public void register(ExtendedStubMapping stubMapping) {
        admin.register(stubMapping);
    }

    @Override
    public void register(StubMapping mapping) {
        ((Admin) admin).addStubMapping(mapping);
    }

    public void register(StringValuePattern scopePath, MappingBuilder mapping) {
        register(scopePath, mapping.build());
    }

    public void register(StringValuePattern scopePath, StubMapping mapping) {
        addScopePathHeader(scopePath, mapping.getRequest());
        ((Admin) admin).addStubMapping(mapping);
    }

    //Others
    public List<RecordedExchange> findMatchingExchanges(StringValuePattern scopePath, ExtendedRequestPattern pattern) {
        addScopePathHeader(scopePath, pattern);
        return admin.findMatchingExchanges(pattern);
    }

    public void resetAll() {
        ((Admin) admin).resetAll();
    }


    public int count(ExtendedRequestPattern requestPattern) {
        return admin.count(requestPattern);
    }

    protected void addScopePathHeader(StringValuePattern scopePath, RequestPattern pattern) {
        if (pattern.getHeaders() == null) {
            setValue(pattern, "headers", new HashMap<>());
        }
        pattern.getHeaders().put(HeaderName.ofTheCorrelationKey(), new MultiValuePattern(scopePath));
    }


    public void registerTemplateVariables(String correlationPath, Map<String, Object> variables) {
        admin.registerTemplateVariables(new CorrelationState(correlationPath,variables));
    }
}
