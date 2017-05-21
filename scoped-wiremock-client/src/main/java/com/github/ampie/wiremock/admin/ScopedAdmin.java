package com.github.ampie.wiremock.admin;

import com.github.tomakehurst.wiremock.matching.RequestPattern;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import com.github.ampie.wiremock.RecordedExchange;

import java.util.List;

public interface ScopedAdmin {
    CorrelationState startNewCorrelatedScope(String parentScopePath);
    CorrelationState joinKnownCorrelatedScope(CorrelationState knownScope);
    CorrelationState getCorrelatedScope(String scopePath);
    List<String> stopCorrelatedScope(String scopePath);
    List<RecordedExchange> findMatchingExchanges(RequestPattern requestPattern);
    void syncCorrelatedScope(CorrelationState correlationState);
    List<StubMapping> getMappingsInScope(String scopePath);

    void startStep(String scopePath, String stepName);

    void stopStep(String scopePath, String stepName);

    List<RecordedExchange> findExchangesAgainstStep(String scopePath, String stepName);
}
