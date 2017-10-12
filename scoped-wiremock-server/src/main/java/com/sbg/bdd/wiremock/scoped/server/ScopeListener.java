package com.sbg.bdd.wiremock.scoped.server;

import com.github.tomakehurst.wiremock.extension.Extension;
import com.sbg.bdd.wiremock.scoped.admin.ScopedAdmin;
import com.sbg.bdd.wiremock.scoped.admin.model.CorrelationState;

public interface ScopeListener extends Extension {
    void setScopedAdmin(ScopedAdmin admin);

    void globalScopeStarted(CorrelationState knownScope);

    void globalScopeStopped(CorrelationState state);

    void nestedScopeStarted(CorrelationState knownScope);

    void nestedScopeStopped(CorrelationState state);

    void stepStarted(CorrelationState state);

    void stepCompleted(CorrelationState state);
}
