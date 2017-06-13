package com.sbg.bdd.wiremock.scoped.admin;

import com.github.tomakehurst.wiremock.matching.RequestPattern;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import com.sbg.bdd.resource.ResourceContainer;
import com.sbg.bdd.wiremock.scoped.admin.model.CorrelationState;
import com.sbg.bdd.wiremock.scoped.admin.model.RecordedExchange;

import java.util.List;

public interface ScopedAdmin {
    void registerResourceRoot(String name, ResourceContainer root);

    void saveRecordingsForRequestPattern(RequestPattern pattern, ResourceContainer recordingDirectory);

    void serveRecordedMappingsAt(ResourceContainer directoryRecordedTo, RequestPattern requestPattern, int priority);

    CorrelationState startNewCorrelatedScope(String parentScopePath);

    CorrelationState joinKnownCorrelatedScope(CorrelationState knownScope);

    CorrelationState getCorrelatedScope(String scopePath);

    List<String> stopCorrelatedScope(CorrelationState state);

    List<RecordedExchange> findMatchingExchanges(RequestPattern requestPattern);

    void syncCorrelatedScope(CorrelationState correlationState);

    List<StubMapping> getMappingsInScope(String scopePath);

    void startStep(CorrelationState state);

    void stopStep(CorrelationState state);

    List<RecordedExchange> findExchangesAgainstStep(String scopePath, String stepName);

    ResourceContainer getResourceRoot(String resourceRoot);
}
