package com.sbg.bdd.wiremock.scoped.server;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.Admin;
import com.github.tomakehurst.wiremock.core.WireMockApp;
import com.github.tomakehurst.wiremock.http.*;
import com.github.tomakehurst.wiremock.matching.RequestPattern;
import com.github.tomakehurst.wiremock.stubbing.InMemoryStubMappings;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.github.tomakehurst.wiremock.stubbing.SortedConcurrentMappingSet;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import com.github.tomakehurst.wiremock.verification.InMemoryRequestJournal;
import com.sbg.bdd.resource.ResourceContainer;
import com.sbg.bdd.wiremock.scoped.admin.ScopedAdmin;
import com.sbg.bdd.wiremock.scoped.admin.model.*;
import com.sbg.bdd.wiremock.scoped.server.recording.ExchangeRecorder;
import com.sbg.bdd.wiremock.scoped.common.ParentPath;
import com.sbg.bdd.wiremock.scoped.integration.HeaderName;
import com.sbg.bdd.wiremock.scoped.server.extended.ServeEventsQueueDecorator;
import com.sbg.bdd.wiremock.scoped.server.extended.SortedConcurrentMappingSetDecorator;
import com.sbg.bdd.wiremock.scoped.server.recording.RecordingManager;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.Pattern;

import static com.sbg.bdd.wiremock.scoped.common.Reflection.getValue;
import static com.sbg.bdd.wiremock.scoped.common.Reflection.setValue;
import static com.sbg.bdd.wiremock.scoped.server.ScopedStubMappings.rectifyRequestHeaders;

public class CorrelatedScopeAdmin implements ScopedAdmin {
    private ServeEventsQueueDecorator requestJournalServedStubs;
    private SortedConcurrentMappingSetDecorator stubMappingsMappings;
    private ExchangeJournal exchangeJournal = new ExchangeJournal();
    private Map<String, GlobalScope> globalScopes = new ConcurrentHashMap<>();
    private ResponseRenderer stubResponseRenderer;
    private ScopeListeners scopeListeners = new ScopeListeners(this, Collections.<String, ScopeListener>emptyMap());
    private Map<String, ResourceContainer> resourceRoots = new HashMap<>();
    private Admin admin;
    private RecordingManager recordingManager = new RecordingManager(this);

    public CorrelatedScopeAdmin() {
    }
    //Scope Management

    @Override
    public GlobalCorrelationState startNewGlobalScope(GlobalCorrelationState globalCorrelationState) {
        int sequenceNumberToUse = -1;
        do {
            globalCorrelationState.setSequenceNumber(++sequenceNumberToUse);
        } while (globalScopes.containsKey(GlobalScope.toKey(globalCorrelationState)));
        GlobalScope newGlobalScope = new GlobalScope(globalCorrelationState);
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
        recordingManager.loadRecordings(nestedScope);
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
        return stubMappingsMappings.findMappingsForScope(scopePath);
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
        new ExchangeRecorder(this, admin).saveRecordingsForRequestPattern(pattern, recordingDirectory);
    }

    @Override
    public void serveRecordedMappingsAt(ResourceContainer directoryRecordedTo, ExtendedRequestPattern requestPattern, int priority) {
        new ExchangeRecorder(this, admin).serveRecordedMappingsAt(directoryRecordedTo, requestPattern, priority);
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
            ExtendedStubMappingCreator creator = new ExtendedStubMappingCreator(extendedStubMapping, scope);
            for (StubMapping mapping : creator.createAllSupportingStubMappings()) {
                admin.addStubMapping(mapping);
            }
        }
        if (extendedStubMapping.getRecordingSpecification() != null) {
            recordingManager.processRecordingSpec(extendedStubMapping, scope);
        }
    }

    public int count(ExtendedRequestPattern pattern) {
        return this.exchangeJournal.count(supportingRequestPatterns(pattern));
    }

    private List<RequestPattern> supportingRequestPatterns(ExtendedRequestPattern pattern) {
        ExtendedStubMappingCreator creator = new ExtendedStubMappingCreator(new ExtendedStubMapping(pattern, null), getAbstractCorrelatedScope(pattern.getCorrelationPath()));
        return creator.createAllSupportingRequestPatterns();
    }

    @Override
    public List<RecordedExchange> findMatchingExchanges(ExtendedRequestPattern pattern) {
        return exchangeJournal.findMatchingExchanges(supportingRequestPatterns(pattern));
    }

    @Override
    public void resetAll() {
        this.exchangeJournal.reset();
        this.stubMappingsMappings.clear();
        this.globalScopes.clear();
        this.requestJournalServedStubs.clear();
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
        InMemoryStubMappings stubMappings = getValue(wireMockApp, "stubMappings");
//TODO we could probably move this hack up into a subclass of InMemoryStubMappings, but it would still be a hack
        SortedConcurrentMappingSet mappingSet = getValue(stubMappings, "mappings");
        this.stubMappingsMappings = new SortedConcurrentMappingSetDecorator(mappingSet);
        setValue(stubMappings, "mappings", this.stubMappingsMappings);
        InMemoryRequestJournal requestJournal = getValue(wireMockApp, "requestJournal");
        ConcurrentLinkedQueue<ServeEvent> serveEvents = getValue(requestJournal, "serveEvents");
        this.requestJournalServedStubs = new ServeEventsQueueDecorator(serveEvents);
        setValue(requestJournal, "serveEvents", this.requestJournalServedStubs);
        final StubRequestHandler stubRequestHandler = getValue(server, "stubRequestHandler");
        //NB!! This hack is necessary to keep track of the potential stack of requests with pending responses
        // A normal ReponseTransformer won't be able to do that because it is always called AFTER the respose
        //is received
        final StubResponseRenderer responseRenderer = getValue(stubRequestHandler, "responseRenderer");
        this.stubResponseRenderer = new ResponseRenderer() {
            @Override
            public Response render(ResponseDefinition responseDefinition) {
                Request request = responseDefinition.getOriginalRequest();
                String scopePath = request.getHeader(HeaderName.ofTheCorrelationKey());
                if (scopePath != null) {
                    //Chop off the segment representing the current user - NB!!! MAJOR ASSUMPTION
                    // Maybe we should change the format of the scope path, something like:
                    // runId:/scope1/scope1.1/scope1.1.1:userId
                    String stepName = determineStep(scopePath);
                    RecordedExchange exchange = exchangeJournal.requestReceived(scopePath, stepName, rectifyRequestHeaders(request));
                    try {
                        Response response = responseRenderer.render(responseDefinition);
                        exchangeJournal.responseReceived(exchange, response);
                        return response;
                    } catch (RuntimeException e) {
                        exchangeJournal.responseReceived(exchange, Response.notConfigured());
                        throw e;
                    }
                } else {
                    return responseRenderer.render(responseDefinition);
                }
            }
        };
        setValue(stubRequestHandler, "responseRenderer", this.stubResponseRenderer);
        setValue(wireMockApp, "stubMappings", new ScopedStubMappings(stubMappings));
    }

    private String determineStep(String scopePath) {
        try {
            String stepContainerPath = scopePath;
            if (scopePath.indexOf("/:") > 0) {
                stepContainerPath = ParentPath.of(scopePath);
            }
            CorrelationState correlationState = getCorrelatedScope(stepContainerPath);
            //CorrelationState could be null if we are not using the scoped client, e.g. testing source systems....
            return correlationState == null ? null : correlationState.getCurrentStep();
        } catch (IllegalArgumentException e) {
            //When a user was not specified and now the correlationPath is too short - only happens in old tests.
            return null;
        }
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
        recordingManager.saveRecordings(scope);
        this.stubMappingsMappings.removeMappingsForScope(scope.getCorrelationPath());
        Pattern pattern = Pattern.compile(scope.getCorrelationPath() + ".*");
        this.requestJournalServedStubs.removeServedStubsForScope(pattern);
        this.exchangeJournal.clearScope(pattern);
    }
}
