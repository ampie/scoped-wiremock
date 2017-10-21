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
import com.sbg.bdd.wiremock.scoped.common.ExchangeRecorder;
import com.sbg.bdd.wiremock.scoped.common.ParentPath;
import com.sbg.bdd.wiremock.scoped.integration.HeaderName;
import com.sbg.bdd.wiremock.scoped.server.extended.ServeEventsQueueDecorator;
import com.sbg.bdd.wiremock.scoped.server.extended.SortedConcurrentMappingSetDecorator;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.Pattern;

import static com.sbg.bdd.wiremock.scoped.common.Reflection.getValue;
import static com.sbg.bdd.wiremock.scoped.common.Reflection.setValue;

public class CorrelatedScopeAdmin implements ScopedAdmin {
    private ServeEventsQueueDecorator requestJournalServedStubs;
    private SortedConcurrentMappingSetDecorator stubMappingsMappings;
    private ExchangeJournal exchangeJournal = new ExchangeJournal();
    private Map<String, GlobalScope> globalScopes = new ConcurrentHashMap<>();
    private ResponseRenderer stubResponseRenderer;
    private ScopeListeners scopeListeners = new ScopeListeners(this, Collections.<String, ScopeListener>emptyMap());
    private Map<String, ResourceContainer> resourceRoots = new HashMap<>();
    private Admin admin;

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
    public GlobalCorrelationState stopGlobalScope(GlobalCorrelationState globalCorrelationState) {
        GlobalScope globalScope = globalScopes.remove(GlobalScope.toKey(globalCorrelationState));
        if (globalScope == null) {
            return null;
        } else {
            stopCorrelatedScope(globalScope.getCorrelationState());
            return globalScope.getCorrelationState();
        }
    }

    @Override
    public CorrelationState startNestedScope(InitialScopeState knownScope) {
        CorrelationState result;
        GlobalScope globalScope = globalScopes.get(CorrelatedScope.globalScopeKey(knownScope.getParentCorrelationPath()));
        if (globalScope == null) {
            result = null;
        } else {
            result = globalScope.findOrCreateNestedScope(knownScope.getParentCorrelationPath(), knownScope.getName()).getCorrelationState();
            result.setPayload(knownScope.getPayload());
        }
        scopeListeners.fireNestedStarted(result);
        return result;
    }

    @Override
    public void syncCorrelatedScope(CorrelationState knownScope) {
        CorrelationState correlationState = getCorrelatedScope(knownScope.getCorrelationPath());
        correlationState.getServiceInvocationCounts().putAll(knownScope.getServiceInvocationCounts());
    }

    @Override
    public List<String> stopCorrelatedScope(CorrelationState state) {
        this.stubMappingsMappings.removeMappingsForScope(state.getCorrelationPath());
        GlobalScope globalScope = globalScopes.get(CorrelatedScope.globalScopeKey(state.getCorrelationPath()));
        if (globalScope == null) {
            return new ArrayList<>();
        } else {
            CorrelatedScope nestedScope = globalScope.findNestedScope(state.getCorrelationPath());
            if (nestedScope instanceof GlobalScope) {
                scopeListeners.fireGlobalScopeStopped(state);
            }else{
                scopeListeners.fireNestedScopeStopped(state);
            }
            Pattern pattern = Pattern.compile(state.getCorrelationPath() + ".*");
            this.requestJournalServedStubs.removeServedStubsForScope(pattern);
            this.exchangeJournal.clearScope(pattern);
            if (nestedScope instanceof GlobalScope) {
                globalScopes.remove(((GlobalScope) nestedScope).getKey());
                return nestedScope.getDescendentCorrelationPaths();
            }
            return nestedScope.getParent().removeChild(nestedScope);
        }
    }

    @Override
    public CorrelationState getCorrelatedScope(String correlationPath) {
        CorrelatedScope correlatedScope = getCorrelatedScopeImpl(correlationPath);
        return correlatedScope == null ? null : correlatedScope.getCorrelationState();
    }

    @Override
    public List<StubMapping> getMappingsInScope(String scopePath) {
        return stubMappingsMappings.findMappingsForScope(scopePath);
    }

    //User scope management
    @Override
    public CorrelationState joinUserScope(InitialScopeState initialScopeState) {
        GlobalScope globalScope = globalScopes.get(CorrelatedScope.globalScopeKey(initialScopeState.getParentCorrelationPath()));
        if (globalScope == null) {
            return null;
        } else {
            CorrelationState state = globalScope.findOrCreateUserScope(initialScopeState.getParentCorrelationPath(), initialScopeState.getName()).getCorrelationState();
            state.setPayload(initialScopeState.getPayload());
            return state;
        }
    }
    @Override
    public CorrelationState stopUserScope(CorrelationState correlationState) {
        this.stopCorrelatedScope(correlationState);
        return correlationState;
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
        ExtendedStubMappingCreator creator = new ExtendedStubMappingCreator(extendedStubMapping, getCorrelatedScopeImpl(extendedStubMapping.getRequest().getCorrelationPath()));
        for (StubMapping mapping : creator.createAllSupportingStubMappings()) {
            admin.addStubMapping(mapping);
        }
    }
    public int count(ExtendedRequestPattern pattern){
        return this.exchangeJournal.count(supportingRequestPatterns(pattern));
    }

    private List<RequestPattern> supportingRequestPatterns(ExtendedRequestPattern pattern) {
        ExtendedStubMappingCreator creator = new ExtendedStubMappingCreator(new ExtendedStubMapping(pattern,null), getCorrelatedScopeImpl(pattern.getCorrelationPath()));
        return creator.createAllSupportingRequestPatterns();
    }

    @Override
    public List<RecordedExchange> findMatchingExchanges(ExtendedRequestPattern pattern) {
        return exchangeJournal.findMatchingExchanges(supportingRequestPatterns(pattern));
    }

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
        StubRequestHandler stubRequestHandler = getValue(server, "stubRequestHandler");
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
                    RecordedExchange exchange = exchangeJournal.requestReceived(scopePath, stepName, request);
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
    }

    private String determineStep(String scopePath) {
        try {
            String stepContainerPath=scopePath;
            if(scopePath.indexOf("/:")>0) {
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


    private CorrelatedScope getCorrelatedScopeImpl(String correlationPath) {
        GlobalScope globalScope = globalScopes.get(CorrelatedScope.globalScopeKey(correlationPath));
        if (globalScope == null) {
            return null;
        } else {
            return globalScope.findNestedScope(correlationPath);
        }
    }

}
