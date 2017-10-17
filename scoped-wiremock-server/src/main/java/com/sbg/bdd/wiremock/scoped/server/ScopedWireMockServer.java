package com.sbg.bdd.wiremock.scoped.server;


import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.Options;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.matching.RequestPattern;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import com.sbg.bdd.resource.ResourceContainer;
import com.sbg.bdd.wiremock.scoped.admin.model.*;
import com.sbg.bdd.wiremock.scoped.admin.ScopedAdmin;
import com.sbg.bdd.wiremock.scoped.common.CanStartAndStop;
import com.sbg.bdd.wiremock.scoped.common.HasBaseUrl;

import java.net.URL;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;


public class ScopedWireMockServer extends WireMockServer implements ScopedAdmin, HasBaseUrl, CanStartAndStop {
    private static Deque<ScopedWireMockServer> locallyRunningServers = new ArrayDeque<>();

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
        scopeAdmin.hackIntoWireMockInternals(this);
        scopeAdmin.setScopeListeners(options.extensionsOfType(ScopeListener.class));
        this.options = options;
    }


    @Override
    public void resetAll() {
        super.resetAll();
        scopeAdmin.resetAll();
    }

    @Override
    public void register(ExtendedStubMapping extendedStubMapping) {
        scopeAdmin.register(extendedStubMapping);
    }
    @Override
    public int count(ExtendedRequestPattern requestPattern) {
       return scopeAdmin.count(requestPattern);
    }

    @Override
    public void registerResourceRoot(String name, ResourceContainer root) {
        scopeAdmin.registerResourceRoot(name, root);
    }

    @Override
    public void saveRecordingsForRequestPattern(ExtendedRequestPattern pattern, ResourceContainer recordingDirectory) {
        scopeAdmin.saveRecordingsForRequestPattern(pattern, recordingDirectory);
    }

    @Override
    public void serveRecordedMappingsAt(ResourceContainer directoryRecordedTo, ExtendedRequestPattern requestPattern, int priority) {
        scopeAdmin.serveRecordedMappingsAt(directoryRecordedTo, requestPattern, priority);
    }

    @Override
    public GlobalCorrelationState startNewGlobalScope(GlobalCorrelationState globalCorrelationState) {
        return scopeAdmin.startNewGlobalScope(globalCorrelationState);
    }

    @Override
    public CorrelationState startNestedScope(CorrelationState knownScope) {
        return scopeAdmin.startNestedScope(knownScope);
    }

    @Override
    public List<String> stopCorrelatedScope(CorrelationState state) {
        return scopeAdmin.stopCorrelatedScope(state);
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
    public List<RecordedExchange> findExchangesAgainstStep(String scopePath, String stepName) {
        return scopeAdmin.findExchangesAgainstStep(scopePath, stepName);
    }

    @Override
    public ResourceContainer getResourceRoot(String resourceRoot) {
        return scopeAdmin.getResourceRoot(resourceRoot);
    }

    @Override
    public GlobalCorrelationState stopGlobalScope(GlobalCorrelationState globalCorrelationState) {
        return scopeAdmin.stopGlobalScope(globalCorrelationState);
    }

    @Override
    public CorrelationState getCorrelatedScope(String correlationPath) {
        return scopeAdmin.getCorrelatedScope(correlationPath);
    }

    @Override
    public void startStep(CorrelationState state) {
        scopeAdmin.startStep(state);
    }

    @Override
    public void stopStep(CorrelationState state) {
        scopeAdmin.stopStep(state);
    }

    @Override
    public List<RecordedExchange> findMatchingExchanges(ExtendedRequestPattern pattern) {
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
        return options.bindAddress().equals("0.0.0.0") ? "localhost" : options.bindAddress();
    }

    @Override
    public String baseUrl() {
        return "http://" + host() + ":" + port();
    }
    private static Options withExtensions(Options options) {
        if (options instanceof WireMockConfiguration) {
            ((WireMockConfiguration) options).extensions(ProxyUrlTransformer.class);
            ((WireMockConfiguration) options).extensions(ScopeExtensions.class);
            ((WireMockConfiguration) options).extensions(InvalidHeadersLoggingTransformer.class);
            ((WireMockConfiguration) options).extensions(ScopeUpdatingResponseTransformer.class);
        } else {
            //TODO Clone it into a WireMockConfiguration

        }
        return options;
    }


}
