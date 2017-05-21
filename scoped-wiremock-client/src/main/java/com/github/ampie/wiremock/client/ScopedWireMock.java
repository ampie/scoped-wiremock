package com.github.ampie.wiremock.client;

import com.github.ampie.wiremock.HasBaseUrl;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.Admin;
import com.github.tomakehurst.wiremock.matching.MultiValuePattern;
import com.github.tomakehurst.wiremock.matching.RequestPattern;
import com.github.tomakehurst.wiremock.matching.StringValuePattern;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import com.github.ampie.wiremock.common.HeaderName;
import com.github.ampie.wiremock.RecordedExchange;
import com.github.ampie.wiremock.admin.CorrelationState;
import com.github.ampie.wiremock.admin.ScopedAdmin;

import java.util.HashMap;
import java.util.List;

import static com.github.ampie.wiremock.common.Reflection.setValue;

public class ScopedWireMock extends WireMock implements HasBaseUrl {

    private static final int DEFAULT_PORT = 8080;
    private static final String DEFAULT_HOST = "localhost";
    protected ScopedAdmin admin;

    public ScopedWireMock(ScopedAdmin admin) {
        super((Admin) admin);
        this.admin = admin;

    }

    public ScopedWireMock(int port) {
        this(DEFAULT_HOST, port);
    }

    public ScopedWireMock(String host, int port) {
        this(new ScopedHttpAdminClient(host, port));
    }

    public ScopedWireMock(String host, int port, String urlPathPrefix) {
        this(new ScopedHttpAdminClient(host, port, urlPathPrefix));
    }

    public ScopedWireMock(String scheme, String host, int port) {
        this(new ScopedHttpAdminClient(scheme, host, port));
    }

    public ScopedWireMock(String scheme, String host, int port, String urlPathPrefix) {
        this(new ScopedHttpAdminClient(scheme, host, port, urlPathPrefix));
    }

    public ScopedWireMock() {
        this(new ScopedHttpAdminClient(DEFAULT_HOST, DEFAULT_PORT));
    }

    public int port() {
        return admin instanceof HasBaseUrl ? ((HasBaseUrl) admin).port() : DEFAULT_PORT;
    }

    public String host() {
        return admin instanceof HasBaseUrl ? ((HasBaseUrl) admin).host() : DEFAULT_HOST;
    }
    @Override
    public String baseUrl(){
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

    public void addScopePathHeader(StringValuePattern scopePath, RequestPattern pattern) {
        if (pattern.getHeaders() == null) {
            setValue(pattern, "headers", new HashMap<>());
        }
        pattern.getHeaders().put(HeaderName.ofTheCorrelationKey(), new MultiValuePattern(scopePath));
    }
}
