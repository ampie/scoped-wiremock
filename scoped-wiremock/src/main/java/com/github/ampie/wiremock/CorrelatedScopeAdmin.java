package com.github.ampie.wiremock;

import com.github.ampie.wiremock.admin.CorrelationState;
import com.github.ampie.wiremock.common.HeaderName;
import com.github.ampie.wiremock.extended.SortedConcurrentMappingSetDecorator;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockApp;
import com.github.tomakehurst.wiremock.http.*;
import com.github.tomakehurst.wiremock.matching.RequestPattern;
import com.github.tomakehurst.wiremock.stubbing.InMemoryStubMappings;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.github.tomakehurst.wiremock.stubbing.SortedConcurrentMappingSet;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import com.github.tomakehurst.wiremock.verification.InMemoryRequestJournal;
import com.github.ampie.wiremock.admin.ScopedAdmin;
import com.github.ampie.wiremock.extended.ServeEventsQueueDecorator;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.Pattern;

import static com.github.ampie.wiremock.common.Reflection.getValue;
import static com.github.ampie.wiremock.common.Reflection.setValue;

public class CorrelatedScopeAdmin implements ScopedAdmin {
    private static final int MAX_CORRELATION_NUMBER_OFFSET = 9999;
    private int highestCorrelationSession = 0;
    private ServeEventsQueueDecorator requestJournalServedStubs;
    private SortedConcurrentMappingSetDecorator stubMappingsMappings;
    private ExchangeJournal exchangeJournal = new ExchangeJournal();
    private Map<String, CorrelationState> correlatedScopes = new HashMap<>();
    private ResponseRenderer stubResponseRenderer;

    public CorrelatedScopeAdmin() {
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
        return findOrCreateCorrelatedScope(knownScope.getCorrelationPath());
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
    public void startStep(String scopePath, String stepName) {
        correlatedScopes.get(scopePath).setCurrentStep(stepName);
    }

    @Override
    public void stopStep(String scopePath, String stepName) {
        if (stepName.indexOf("/") > 0) {
            correlatedScopes.get(scopePath).setCurrentStep(stepName.substring(0, stepName.lastIndexOf("/")));
        } else {
            correlatedScopes.get(scopePath).setCurrentStep(null);
        }
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

    public List<String> stopCorrelatedScope(String scopePath) {
        this.stubMappingsMappings.removeMappingsForScope(scopePath);
        Pattern pattern = Pattern.compile(scopePath + ".*");
        this.requestJournalServedStubs.removeServedStubsForScope(pattern);
        this.exchangeJournal.clearScope(pattern);
        Set<String> removedCorrelationPaths = extractAffectededScopePaths(pattern);
        removeScopeAndChildren(pattern);
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

    public Set<String> getActiveCorrelationPaths() {
        return correlatedScopes.keySet();
    }


    private int internalNextPossibleCorrelationNumber() {
        highestCorrelationSession = highestCorrelationSession == MAX_CORRELATION_NUMBER_OFFSET ? 0 : highestCorrelationSession + 1;
        return highestCorrelationSession;
    }

    public void decorateRelevantCollections(WireMockServer server) {
        WireMockApp wireMockApp = getValue(server, "wireMockApp");
        InMemoryStubMappings stubMappings = getValue(wireMockApp, "stubMappings");
        SortedConcurrentMappingSet mappingSet = getValue(stubMappings, "mappings");
        this.stubMappingsMappings = new SortedConcurrentMappingSetDecorator(mappingSet);
        setValue(stubMappings, "mappings", this.stubMappingsMappings);
        InMemoryRequestJournal requestJournal = getValue(wireMockApp, "requestJournal");
        ConcurrentLinkedQueue<ServeEvent> serveEvents = getValue(requestJournal, "serveEvents");
        this.requestJournalServedStubs = new ServeEventsQueueDecorator(serveEvents);
        setValue(requestJournal, "serveEvents", this.requestJournalServedStubs);
        StubRequestHandler stubRequestHandler = getValue(server, "stubRequestHandler");
        final StubResponseRenderer responseRenderer = getValue(stubRequestHandler, "responseRenderer");
        this.stubResponseRenderer = new ResponseRenderer() {
            @Override
            public Response render(ResponseDefinition responseDefinition) {
                Request request = responseDefinition.getOriginalRequest();
                String scopePath = request.getHeader(HeaderName.ofTheCorrelationKey());
                CorrelationState correlationState = correlatedScopes.get(ExecutionPathExtractor.executionScopePathFrom(scopePath));
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
}
