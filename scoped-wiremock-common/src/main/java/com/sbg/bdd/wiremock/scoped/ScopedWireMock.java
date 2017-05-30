package com.sbg.bdd.wiremock.scoped;

import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.Admin;
import com.github.tomakehurst.wiremock.matching.MultiValuePattern;
import com.github.tomakehurst.wiremock.matching.RequestPattern;
import com.github.tomakehurst.wiremock.matching.StringValuePattern;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import com.sbg.bdd.wiremock.scoped.admin.ScopedAdmin;
import com.sbg.bdd.wiremock.scoped.admin.model.CorrelationState;
import com.sbg.bdd.wiremock.scoped.admin.model.RecordedExchange;
import com.sbg.bdd.wiremock.scoped.common.CanStartAndStop;
import com.sbg.bdd.wiremock.scoped.common.HasBaseUrl;
import com.sbg.bdd.wiremock.scoped.integration.HeaderName;

import java.util.HashMap;
import java.util.List;

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
        return admin.joinKnownCorrelatedScope(new CorrelationState(knownScopePath));
    }

    public CorrelationState startNewCorrelatedScope(String knownScopePath) {
        return admin.startNewCorrelatedScope(knownScopePath);
    }

    public List<String> stopCorrelatedScope(String knownScopePath) {
        return admin.stopCorrelatedScope(knownScopePath);
    }

    public CorrelationState getCorrelatedScope(String scopePath) {
        return admin.getCorrelatedScope(scopePath);
    }

    public void syncCorrelatedScope(CorrelationState nestedScope) {
        admin.syncCorrelatedScope(nestedScope);
    }

    //Step management
    public void startStep(String scopePath, String stepName) {
        admin.startStep(scopePath, stepName);
    }

    public List<RecordedExchange> findExchangesAgainstStep(String scopePath, String stepName) {
        return admin.findExchangesAgainstStep(scopePath, stepName);
    }

    public void stopStep(String scopePath, String stepName) {
        admin.stopStep(scopePath, stepName);

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

    private void addScopePathHeader(StringValuePattern scopePath, RequestPattern pattern) {
        if (pattern.getHeaders() == null) {
            setValue(pattern, "headers", new HashMap<>());
        }
        pattern.getHeaders().put(HeaderName.ofTheCorrelationKey(), new MultiValuePattern(scopePath));
    }
}
