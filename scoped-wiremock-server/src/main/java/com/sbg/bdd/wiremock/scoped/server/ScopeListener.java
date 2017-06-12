package com.sbg.bdd.wiremock.scoped.server;

import com.github.tomakehurst.wiremock.extension.Extension;
import com.sbg.bdd.wiremock.scoped.admin.model.CorrelationState;

public interface ScopeListener extends Extension {
    void scopeStarted(CorrelationState knownScope);

    void scopeStopped(CorrelationState state);

    void stepStarted(CorrelationState state);

    void stepCompleted(CorrelationState state);
}
