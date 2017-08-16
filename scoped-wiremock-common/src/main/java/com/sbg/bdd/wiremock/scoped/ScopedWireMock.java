package com.sbg.bdd.wiremock.scoped;

import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.Admin;
import com.github.tomakehurst.wiremock.matching.MultiValuePattern;
import com.github.tomakehurst.wiremock.matching.RequestPattern;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import com.github.tomakehurst.wiremock.matching.StringValuePattern;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import com.sbg.bdd.resource.ResourceContainer;
import com.sbg.bdd.wiremock.scoped.admin.ScopedAdmin;
import com.sbg.bdd.wiremock.scoped.admin.model.CorrelationState;
import com.sbg.bdd.wiremock.scoped.admin.model.RecordedExchange;
import com.sbg.bdd.wiremock.scoped.common.CanStartAndStop;
import com.sbg.bdd.wiremock.scoped.common.ExchangeRecorder;
import com.sbg.bdd.wiremock.scoped.common.HasBaseUrl;
import com.sbg.bdd.wiremock.scoped.integration.HeaderName;

import java.util.Collections;
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
    public CorrelationState joinCorrelatedScope(String knownScopePath) {
        return joinCorrelatedScope(knownScopePath, Collections.<String, Object>emptyMap());
    }
    public CorrelationState joinCorrelatedScope(String knownScopePath,Map<String,Object> payload) {
        return admin.joinKnownCorrelatedScope(new CorrelationState(knownScopePath,payload));
    }

    public CorrelationState startNewCorrelatedScope(String knownScopePath) {
        return admin.startNewCorrelatedScope(knownScopePath);
    }

    public List<String> stopCorrelatedScope(String knownScopePath) {
        return stopCorrelatedScope(knownScopePath, Collections.<String,Object>emptyMap());
    }
    public int count(RequestPatternBuilder requestPatternBuilder) {
        Admin admin = (Admin) this.admin;
        return admin.countRequestsMatching(requestPatternBuilder.build()).getCount();
    }
    public List<String> stopCorrelatedScope(String knownScopePath, Map<String, Object> map) {
        return admin.stopCorrelatedScope(new CorrelationState(knownScopePath,map));
    }

    public CorrelationState getCorrelatedScope(String scopePath) {
        return admin.getCorrelatedScope(scopePath);
    }

    public void syncCorrelatedScope(CorrelationState nestedScope) {
        admin.syncCorrelatedScope(nestedScope);
    }

    //Step management
    public void startStep(String scopePath, String stepName) {
        startStep(scopePath, stepName, Collections.<String, Object>emptyMap());
    }
    public void startStep(String scopePath, String stepName, Map<String,Object> payload) {
        admin.startStep(new CorrelationState(scopePath,stepName,payload));
    }

    public List<RecordedExchange> findExchangesAgainstStep(String scopePath, String stepName) {
        return admin.findExchangesAgainstStep(scopePath, stepName);
    }

    public void stopStep(String scopePath, String stepName) {
        stopStep(scopePath, stepName, Collections.<String, Object>emptyMap());

    }
    public void stopStep(String scopePath, String stepName, Map<String,Object> payload) {
        admin.stopStep(new CorrelationState(scopePath,stepName,payload));

    }

    //Others
    public List<StubMapping> getMappingsInScope(String scopePath) {
        return admin.getMappingsInScope(scopePath);
    }

    public List<RecordedExchange> findMatchingExchanges(StringValuePattern scopePath, RequestPattern pattern) {
        addScopePathHeader(scopePath, pattern);
        return admin.findMatchingExchanges(pattern);
    }

    public void resetAll() {
        ((Admin) admin).resetAll();
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

    protected void addScopePathHeader(StringValuePattern scopePath, RequestPattern pattern) {
        if (pattern.getHeaders() == null) {
            setValue(pattern, "headers", new HashMap<>());
        }
        pattern.getHeaders().put(HeaderName.ofTheCorrelationKey(), new MultiValuePattern(scopePath));
    }
    public void saveRecordingsForRequestPattern(StringValuePattern scopePath, RequestPattern pattern, ResourceContainer recordingDirectory) {
        addScopePathHeader(scopePath, pattern);
        admin.saveRecordingsForRequestPattern(pattern, recordingDirectory);
    }

    public void  serveRecordedMappingsAt(ResourceContainer directoryRecordedTo, RequestPattern requestPattern, int priority) {
        admin.serveRecordedMappingsAt(directoryRecordedTo, requestPattern, priority);
    }
}
