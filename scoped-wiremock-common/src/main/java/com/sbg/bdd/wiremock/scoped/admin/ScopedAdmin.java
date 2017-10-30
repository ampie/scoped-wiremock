package com.sbg.bdd.wiremock.scoped.admin;

import com.github.tomakehurst.wiremock.matching.RequestPattern;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import com.sbg.bdd.resource.ResourceContainer;
import com.sbg.bdd.wiremock.scoped.admin.model.*;

import java.net.URL;
import java.util.List;

public interface ScopedAdmin {
    String PERSONA_RESOURCE_ROOT = "personaResourceRoot";
    String JOURNAL_RESOURCE_ROOT = "journalResourceRoot";
    String OUTPUT_RESOURCE_ROOT = "outputResourceRoot";
    String INPUT_RESOURCE_ROOT = "inputResourceRoot";
    String GUEST = "guest";

    //Recording management
    void saveRecordingsForRequestPattern(ExtendedRequestPattern pattern, ResourceContainer recordingDirectory);

    void serveRecordedMappingsAt(ResourceContainer directoryRecordedTo, ExtendedRequestPattern requestPattern, int priority);

    //Scope management
    GlobalCorrelationState startNewGlobalScope(GlobalCorrelationState globalCorrelationState);

    GlobalCorrelationState stopGlobalScope(GlobalCorrelationState globalCorrelationState);

    CorrelationState startNestedScope(InitialScopeState initialScopeState);

    CorrelationState getCorrelatedScope(String scopePath);

    //NB! we never remove user scopes directly, the get removed when the nested scope is removed
    CorrelationState startUserScope(InitialScopeState initialScopeState);

    List<String> stopNestedScope(CorrelationState state);

    void syncCorrelatedScope(CorrelationState correlationState);

    List<StubMapping> getMappingsInScope(String scopePath);

    //Step Management
    void startStep(CorrelationState state);

    void stopStep(CorrelationState state);

    List<RecordedExchange> findExchangesAgainstStep(String scopePath, String stepName);

    //Resources
    void registerResourceRoot(String name, ResourceContainer root);

    ResourceContainer getResourceRoot(String resourceRoot);

    //Others
    void register(ExtendedStubMapping extendedStubMapping);

    int count(ExtendedRequestPattern requestPattern);

    List<RecordedExchange> findMatchingExchanges(ExtendedRequestPattern requestPattern);

    void resetAll();
}
