package com.sbg.bdd.wiremock.scoped;


import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.Options;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.matching.RequestPattern;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import com.sbg.bdd.wiremock.scoped.admin.model.CorrelationState;
import com.sbg.bdd.wiremock.scoped.admin.ScopedAdmin;
import com.sbg.bdd.wiremock.scoped.common.CanStartAndStop;
import com.sbg.bdd.wiremock.scoped.common.HasBaseUrl;
import com.sbg.bdd.wiremock.scoped.admin.model.RecordedExchange;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;


public class ScopedWireMockServer extends WireMockServer implements ScopedAdmin, HasBaseUrl, CanStartAndStop {
    private static Deque<ScopedWireMockServer> locallyRunningServers=new ArrayDeque<>();

    private final Options options;
    private CorrelatedScopeAdmin scopeAdmin;

    public static boolean isRunningLocally() {
        return !locallyRunningServers.isEmpty();
    }
    public static ScopedWireMockServer getCurrentLocalServer() {
        return locallyRunningServers.peek();
    }
    public ScopedWireMockServer(Integer port) {
        this(new WireMockConfiguration().port(port));
    }

    public ScopedWireMockServer(Integer port, WireMockConfiguration options) {
        this(options.port(port));
    }

    public ScopedWireMockServer() {
        this(WireMockConfiguration.DYNAMIC_PORT);
    }


    public ScopedWireMockServer(Options options) {
        super(withExtensions(options));
        scopeAdmin = (CorrelatedScopeAdmin) ScopeExtensions.getCurrentAdmin();
        scopeAdmin.decorateRelevantCollections(this);
        this.options=options;
    }


    private static Options withExtensions(Options options) {
        if(options instanceof WireMockConfiguration) {
            ((WireMockConfiguration)options).extensions(ProxyUrlTransformer.class);
            ((WireMockConfiguration)options).extensions(ScopeExtensions.class);
            ((WireMockConfiguration)options).extensions(InvalidHeadersLoggingTransformer.class);
            ((WireMockConfiguration)options).extensions(ScopeUpdatingResponseTransformer.class);
        }else{
            //TODO Clone it into a WireMockConfiguration

        }
        return options;
    }

    @Override
    public void resetAll() {
        super.resetAll();
        scopeAdmin.resetAll();
    }

    @Override
    public CorrelationState startNewCorrelatedScope(String parentScopePath) {
        return scopeAdmin.startNewCorrelatedScope(parentScopePath);
    }

    @Override
    public CorrelationState joinKnownCorrelatedScope(CorrelationState knownScope) {
        return scopeAdmin.joinKnownCorrelatedScope(knownScope);
    }

    @Override
    public void syncCorrelatedScope(CorrelationState knownScope) {
        scopeAdmin.syncCorrelatedScope(knownScope);
    }

    @Override
    public List<StubMapping> getMappingsInScope(String scopePath) {
        return scopeAdmin.getMappingsInScope(scopePath);
    }

    @Override
    public void startStep(String scopePath, String stepName) {
        scopeAdmin.startStep(scopePath, stepName);
    }

    @Override
    public void stopStep(String scopePath, String stepName) {
        scopeAdmin.stopStep(scopePath, stepName);
    }

    @Override
    public List<RecordedExchange> findExchangesAgainstStep(String scopePath, String stepName) {
        return scopeAdmin.findExchangesAgainstStep(scopePath, stepName);
    }

    @Override
    public CorrelationState getCorrelatedScope(String correlationPath) {
        return scopeAdmin.getCorrelatedScope(correlationPath);
    }


    @Override
    public List<String> stopCorrelatedScope(String scopePath) {
        return scopeAdmin.stopCorrelatedScope(scopePath);
    }

    @Override
    public List<RecordedExchange> findMatchingExchanges(RequestPattern pattern) {
        return scopeAdmin.findMatchingExchanges(pattern);
    }

    public void stop() {
        super.stop();
        locallyRunningServers.pop();
    }

    @Override
    public void start() {
        super.start();
        locallyRunningServers.push(this);
    }

    @Override
    public String host() {
        return options.bindAddress().equals("0.0.0.0")?"localhost":options.bindAddress();
    }
    @Override
    public String baseUrl(){
        return "http://" + host() + ":" + port();
    }
}
