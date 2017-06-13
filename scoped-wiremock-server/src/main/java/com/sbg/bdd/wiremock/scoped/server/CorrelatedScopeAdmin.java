package com.sbg.bdd.wiremock.scoped.server;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
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
import com.sbg.bdd.wiremock.scoped.admin.model.CorrelationState;
import com.sbg.bdd.wiremock.scoped.admin.model.RecordedExchange;
import com.sbg.bdd.wiremock.scoped.common.ExchangeRecorder;
import com.sbg.bdd.wiremock.scoped.common.ParentPath;
import com.sbg.bdd.wiremock.scoped.integration.HeaderName;
import com.sbg.bdd.wiremock.scoped.server.extended.ServeEventsQueueDecorator;
import com.sbg.bdd.wiremock.scoped.server.extended.SortedConcurrentMappingSetDecorator;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.Pattern;

import static com.sbg.bdd.wiremock.scoped.common.Reflection.getValue;
import static com.sbg.bdd.wiremock.scoped.common.Reflection.setValue;

public class CorrelatedScopeAdmin implements ScopedAdmin {
    private static final int MAX_CORRELATION_NUMBER_OFFSET = 9999;
    private int highestCorrelationSession = 0;
    private ServeEventsQueueDecorator requestJournalServedStubs;
    private SortedConcurrentMappingSetDecorator stubMappingsMappings;
    private ExchangeJournal exchangeJournal = new ExchangeJournal();
    private Map<String, CorrelationState> correlatedScopes = new HashMap<>();
    private ResponseRenderer stubResponseRenderer;
    private ScopeListeners scopeListeners = new ScopeListeners(Collections.<String, ScopeListener>emptyMap());
    private Map<String, ResourceContainer> resourceRoots = new HashMap<>();
    private Admin admin;

    public CorrelatedScopeAdmin() {
    }


    @Override
    public void registerResourceRoot(String name, ResourceContainer root) {
        this.resourceRoots.put(name, root);
    }

    @Override
    public ResourceContainer getResourceRoot(String resourceRoot) {
        return resourceRoots.get(resourceRoot);
    }

    @Override
    public void saveRecordingsForRequestPattern(RequestPattern pattern, ResourceContainer recordingDirectory) {
        new ExchangeRecorder(this,admin).saveRecordingsForRequestPattern(pattern, recordingDirectory);
    }

    @Override
    public void serveRecordedMappingsAt(ResourceContainer directoryRecordedTo, RequestPattern requestPattern, int priority) {
        new ExchangeRecorder(this,admin).serveRecordedMappingsAt(directoryRecordedTo, requestPattern, priority);
    }

    @Override
    public CorrelationState startNewCorrelatedScope(String parentScopePath) {
        String correlationPathToUse = null;
        do {
            correlationPathToUse = parentScopePath + "/" + internalNextPossibleCorrelationNumber();
        } while (correlatedScopes.containsKey(correlationPathToUse));
        return findOrCreateCorrelatedScope(correlationPathToUse);
    }

    @Override
    public CorrelationState joinKnownCorrelatedScope(CorrelationState knownScope) {
        CorrelationState state = findOrCreateCorrelatedScope(knownScope.getCorrelationPath());
        scopeListeners.fireScopeStarted(knownScope);
        return state;
    }

    @Override
    public void syncCorrelatedScope(CorrelationState knownScope) {
        CorrelationState correlationState = getCorrelatedScope(knownScope.getCorrelationPath());
        correlationState.getServiceInvocationCounts().putAll(knownScope.getServiceInvocationCounts());
    }

    @Override
    public List<StubMapping> getMappingsInScope(String scopePath) {
        return stubMappingsMappings.findMappingsForScope(scopePath);
    }

    @Override
    public void startStep(CorrelationState state) {
        correlatedScopes.get(state.getCorrelationPath()).setCurrentStep(state.getCurrentStep());
        scopeListeners.fireStepStarted(state);
    }

    @Override
    public void stopStep(CorrelationState state) {
        correlatedScopes.get(state.getCorrelationPath()).setCurrentStep(ParentPath.of(state.getCurrentStep()));
        scopeListeners.fireStepCompleted(state);
    }

    @Override
    public List<RecordedExchange> findExchangesAgainstStep(String scopePath, String stepName) {
        return exchangeJournal.findExchangesAgainstStep(scopePath, stepName);
    }

    public CorrelationState getCorrelatedScope(String correlationPath) {
        return correlatedScopes.get(correlationPath);
    }

    public CorrelationState findOrCreateCorrelatedScope(String correlationPath) {
        CorrelationState result = correlatedScopes.get(correlationPath);
        if (result == null) {
            correlatedScopes.put(correlationPath, result = new CorrelationState(correlationPath));
        }
        return result;
    }

    public List<String> stopCorrelatedScope(CorrelationState state) {
        this.stubMappingsMappings.removeMappingsForScope(state.getCorrelationPath());
        Pattern pattern = Pattern.compile(state.getCorrelationPath() + ".*");
        this.requestJournalServedStubs.removeServedStubsForScope(pattern);
        this.exchangeJournal.clearScope(pattern);
        Set<String> removedCorrelationPaths = extractAffectededScopePaths(pattern);
        removeScopeAndChildren(pattern);
        scopeListeners.fireScopeStopped(state);
        return new ArrayList<>(removedCorrelationPaths);
    }

    private Set<String> extractAffectededScopePaths(Pattern pattern) {
        Set<String> result = new HashSet<>();
        for (String s : this.correlatedScopes.keySet()) {
            if (pattern.matcher(s).find()) {
                result.add(s);
            }
        }
        return result;
    }

    private void removeScopeAndChildren(Pattern pattern) {
        Iterator<Map.Entry<String, CorrelationState>> iterator = this.correlatedScopes.entrySet().iterator();
        while (iterator.hasNext()) {
            if (pattern.matcher(iterator.next().getKey()).find()) {
                iterator.remove();
            }
        }
    }

    public List<RecordedExchange> findMatchingExchanges(RequestPattern pattern) {
        return exchangeJournal.findMatchingExchanges(pattern);
    }

    private int internalNextPossibleCorrelationNumber() {
        highestCorrelationSession = highestCorrelationSession == MAX_CORRELATION_NUMBER_OFFSET ? 0 : highestCorrelationSession + 1;
        return highestCorrelationSession;
    }

    /**
     * A lot of hacks to ensure that all in scope mappings and request journal entries are removed when a scope
     * is completed
     *
     * @param server
     */
    public void hackIntoWireMockInternals(WireMockServer server) {
        admin=server;
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
                //Chop off the segment representing the current user - NB!!! MAJOR ASSUMPTION
                // Maybe we should change the format of the scope path, something like:
                // runId:/scope1/scope1.1/scope1.1.1:userId
                String stepContainerPath = ParentPath.of(scopePath);
                CorrelationState correlationState = correlatedScopes.get(stepContainerPath);
                //CorrelationState could be null if we are not using the scoped client, e.g. testing source systems....
                String stepName = correlationState == null ? null : correlationState.getCurrentStep();
                RecordedExchange exchange = exchangeJournal.requestReceived(scopePath, stepName, request);
                try {
                    Response response = responseRenderer.render(responseDefinition);
                    exchangeJournal.responseReceived(exchange, response);
                    return response;
                } catch (RuntimeException e) {
                    exchangeJournal.responseReceived(exchange, Response.notConfigured());
                    throw e;
                }
            }
        };
        setValue(stubRequestHandler, "responseRenderer", this.stubResponseRenderer);
    }


    public void resetAll() {
        this.exchangeJournal.reset();
        this.stubMappingsMappings.clear();
        this.correlatedScopes.clear();
        this.requestJournalServedStubs.clear();
        this.highestCorrelationSession = 0;
    }

    public void setScopeListeners(Map<String, ScopeListener> scopeListeners) {
        this.scopeListeners = new ScopeListeners(scopeListeners);
    }
}
