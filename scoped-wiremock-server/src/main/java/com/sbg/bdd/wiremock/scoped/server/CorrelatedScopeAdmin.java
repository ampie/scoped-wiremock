package com.sbg.bdd.wiremock.scoped.server;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.Admin;
import com.github.tomakehurst.wiremock.core.WireMockApp;
import com.github.tomakehurst.wiremock.http.*;
import com.github.tomakehurst.wiremock.matching.RequestPattern;
import com.github.tomakehurst.wiremock.stubbing.InMemoryStubMappings;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import com.github.tomakehurst.wiremock.verification.InMemoryRequestJournal;
import com.sbg.bdd.resource.ResourceContainer;
import com.sbg.bdd.wiremock.scoped.admin.ScopedAdmin;
import com.sbg.bdd.wiremock.scoped.admin.model.*;
import com.sbg.bdd.wiremock.scoped.common.ParentPath;
import com.sbg.bdd.wiremock.scoped.server.decorated.InMemoryRequestJournalDecorator;
import com.sbg.bdd.wiremock.scoped.server.decorated.InMemoryStubMappingsDecorator;
import com.sbg.bdd.wiremock.scoped.server.decorated.StubResponseRendererDecorator;
import com.sbg.bdd.wiremock.scoped.server.recording.ExchangeRecorder;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import static com.sbg.bdd.wiremock.scoped.common.Reflection.getValue;
import static com.sbg.bdd.wiremock.scoped.common.Reflection.setValue;

public class CorrelatedScopeAdmin implements ScopedAdmin {
    private ExchangeJournal exchangeJournal = new ExchangeJournal();
    private Map<String, GlobalScope> globalScopes = new ConcurrentHashMap<>();
    private ResponseRenderer stubResponseRenderer;
    private ScopeListeners scopeListeners = new ScopeListeners(this, Collections.<String, ScopeListener>emptyMap());
    private Map<String, ResourceContainer> resourceRoots = new HashMap<>();
    private Admin admin;
    private ExchangeRecorder exchangeRecorder = new ExchangeRecorder(this);
    private InMemoryRequestJournalDecorator requestJournal;
    private InMemoryStubMappingsDecorator stubMappings;

    public CorrelatedScopeAdmin() {
    }
    //Scope Management

    @Override
    public GlobalCorrelationState startNewGlobalScope(GlobalCorrelationState globalCorrelationState) {
        int sequenceNumberToUse = -1;
        do {
            globalCorrelationState.setSequenceNumber(++sequenceNumberToUse);
        } while (globalScopes.containsKey(GlobalScope.toKey(globalCorrelationState)));
        GlobalScope newGlobalScope = new GlobalScope(globalCorrelationState,exchangeRecorder.allPersonaIds());
        globalScopes.put(newGlobalScope.getKey(), newGlobalScope);
        scopeListeners.fireGlobalStarted(newGlobalScope.getCorrelationState());
        return newGlobalScope.getCorrelationState();
    }

    @Override
    public GlobalCorrelationState stopGlobalScope(GlobalCorrelationState state) {
        GlobalScope globalScope = globalScopes.get(GlobalScope.toKey(state));
        globalScope.getCorrelationState().setPayload(state.getPayload());
        scopeListeners.fireGlobalScopeStopped(globalScope.getCorrelationState());
        finalizeScope(globalScope);
        return globalScopes.remove(GlobalScope.toKey(state)).getCorrelationState();
    }


    @Override
    public CorrelationState startNestedScope(InitialScopeState initialScopeState) {
        GlobalScope globalScope = globalScopes.get(CorrelatedScope.globalScopeKey(initialScopeState.getParentCorrelationPath()));
        CorrelatedScope nestedScope = globalScope.findOrCreateNestedScope(initialScopeState.getParentCorrelationPath(), initialScopeState.getName());
        nestedScope.getCorrelationState().setPayload(initialScopeState.getPayload());
        exchangeRecorder.loadRecordings(nestedScope);
        scopeListeners.fireNestedStarted(nestedScope.getCorrelationState());
        return nestedScope.getCorrelationState();
    }

    @Override
    public void syncCorrelatedScope(CorrelationState knownScope) {
        CorrelationState correlationState = getCorrelatedScope(knownScope.getCorrelationPath());
        correlationState.putServiceInvocationCounts(knownScope.getServiceInvocationCounts());
    }

    @Override
    public List<String> stopNestedScope(CorrelationState state) {
        GlobalScope globalScope = globalScopes.get(CorrelatedScope.globalScopeKey(state.getCorrelationPath()));
        CorrelatedScope nestedScope = globalScope.findNestedScopeRecursively(state.getCorrelationPath());
        nestedScope.getCorrelationState().setPayload(state.getPayload());
        scopeListeners.fireNestedScopeStopped(nestedScope.getCorrelationState());
        finalizeScope(nestedScope);
        return nestedScope.getParent().removeNestedScope(nestedScope);
    }

    @Override
    public CorrelationState getCorrelatedScope(String correlationPath) {
        AbstractCorrelatedScope correlatedScope = getAbstractCorrelatedScope(correlationPath);
        return correlatedScope == null ? null : correlatedScope.getCorrelationState();
    }

    @Override
    public void registerTemplateVariables(CorrelationState state) {
        getAbstractCorrelatedScope(state.getCorrelationPath()).getTemplateVariables().putAll(state.getPayload());
    }

    @Override
    public List<StubMapping> getMappingsInScope(String scopePath) {
        return stubMappings.findMappingsForScope(scopePath);
    }

    //User scope management
    @Override
    public CorrelationState startUserScope(InitialScopeState initialScopeState) {
        GlobalScope globalScope = globalScopes.get(CorrelatedScope.globalScopeKey(initialScopeState.getParentCorrelationPath()));
        UserScope userScope = globalScope.findOrCreateUserScope(initialScopeState.getParentCorrelationPath(), initialScopeState.getName());
        userScope.getCorrelationState().setPayload(initialScopeState.getPayload());
        return userScope.getCorrelationState();
    }

    //Resources
    @Override
    public void registerResourceRoot(String name, ResourceContainer root) {
        this.resourceRoots.put(name, root);
    }

    @Override
    public ResourceContainer getResourceRoot(String resourceRoot) {
        return resourceRoots.get(resourceRoot);
    }

    //Recording Management
    @Override
    public void saveRecordingsForRequestPattern(ExtendedRequestPattern pattern, ResourceContainer recordingDirectory) {
        new ExchangeRecorder(this).saveRecordingsForRequestPattern(pattern, recordingDirectory);
    }

    @Override
    public void serveRecordedMappingsAt(ResourceContainer directoryRecordedTo, ExtendedRequestPattern requestPattern, int priority) {
        new ExchangeRecorder(this).serveRecordedMappingsAt(directoryRecordedTo, requestPattern, priority);
    }

    //Step management
    @Override
    public void startStep(CorrelationState state) {
        getCorrelatedScope(state.getCorrelationPath()).setCurrentStep(state.getCurrentStep());
        scopeListeners.fireStepStarted(state);
    }

    @Override
    public void stopStep(CorrelationState state) {
        getCorrelatedScope(state.getCorrelationPath()).setCurrentStep(ParentPath.of(state.getCurrentStep()));
        scopeListeners.fireStepCompleted(state);
    }

    //Others
    @Override
    public List<RecordedExchange> findExchangesAgainstStep(String scopePath, String stepName) {
        return exchangeJournal.findExchangesAgainstStep(scopePath, stepName);
    }

    @Override
    public void register(ExtendedStubMapping extendedStubMapping) {
        AbstractCorrelatedScope scope = getAbstractCorrelatedScope(extendedStubMapping.getRequest().getCorrelationPath());
        if (extendedStubMapping.getExtendedResponse() != null) {
            ExtendedStubMappingTranslator creator = new ExtendedStubMappingTranslator(extendedStubMapping, scope);
            for (StubMapping mapping : creator.createAllSupportingStubMappings()) {
                addStubMapping(mapping);
            }
        }
        if (extendedStubMapping.getRecordingSpecification() != null) {
            exchangeRecorder.processRecordingSpec(extendedStubMapping, scope);
        }
    }

    @Override
    public void addStubMapping(StubMapping stubMapping) {
        admin.addStubMapping(stubMapping);
    }

    public int count(ExtendedRequestPattern pattern) {
        return this.exchangeJournal.count(supportingRequestPatterns(pattern));
    }

    private List<RequestPattern> supportingRequestPatterns(ExtendedRequestPattern pattern) {
        ExtendedStubMappingTranslator creator = new ExtendedStubMappingTranslator(new ExtendedStubMapping(pattern, null), getAbstractCorrelatedScope(pattern.getCorrelationPath()));
        return creator.createAllSupportingRequestPatterns();
    }

    @Override
    public List<RecordedExchange> findMatchingExchanges(ExtendedRequestPattern pattern) {
        return exchangeJournal.findMatchingExchanges(supportingRequestPatterns(pattern));
    }

    @Override
    public void resetAll() {
        this.exchangeJournal.reset();
        this.stubMappings.reset();
        this.globalScopes.clear();
        this.requestJournal.reset();
    }

    public void setScopeListeners(Map<String, ScopeListener> scopeListeners) {
        this.scopeListeners = new ScopeListeners(this, scopeListeners);
    }

    /**
     * A lot of hacks to ensure that all in scope mappings and request journal entries are removed when a scope
     * is completed
     *
     * @param server
     */
    public void hackIntoWireMockInternals(WireMockServer server) {
        admin = server;
        WireMockApp wireMockApp = getValue(server, "wireMockApp");
        decorateStubMappings(wireMockApp);
        decorateRequestJournal(wireMockApp);
        decorateStubRequestHandler(server);
    }

    private void decorateStubRequestHandler(WireMockServer server) {
        StubRequestHandler stubRequestHandler = getValue(server, "stubRequestHandler");
        final StubResponseRenderer responseRenderer = getValue(stubRequestHandler, "responseRenderer");
        this.stubResponseRenderer = new StubResponseRendererDecorator(responseRenderer, this, exchangeJournal);
        setValue(stubRequestHandler, "responseRenderer", this.stubResponseRenderer);
    }

    private void decorateRequestJournal(WireMockApp wireMockApp) {
        InMemoryRequestJournal requestJournal = getValue(wireMockApp, "requestJournal");
        this.requestJournal = new InMemoryRequestJournalDecorator(requestJournal);
        setValue(wireMockApp,"requestJournal", this.requestJournal);
    }

    private void decorateStubMappings(WireMockApp wireMockApp) {
        InMemoryStubMappings stubMappings = getValue(wireMockApp, "stubMappings");
        this.stubMappings =  new InMemoryStubMappingsDecorator(stubMappings);
        setValue(wireMockApp,"stubMappings",this.stubMappings);
    }

    public AbstractCorrelatedScope getAbstractCorrelatedScope(String correlationPath) {
        GlobalScope globalScope = globalScopes.get(CorrelatedScope.globalScopeKey(correlationPath));
        if (globalScope == null || globalScope.getCorrelationPath().equals(correlationPath)) {
            return globalScope;
        } else {
            return globalScope.findScopeRecursively(correlationPath);
        }
    }

    private void finalizeScope(CorrelatedScope scope) {
        exchangeRecorder.saveRecordings(scope);
        this.stubMappings.removeMappingsForScope(scope.getCorrelationPath());
        Pattern pattern = Pattern.compile(scope.getCorrelationPath() + ".*");
        this.requestJournal.removeServedStubsForScope(pattern);
        this.exchangeJournal.clearScope(pattern);
    }

}
