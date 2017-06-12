package com.sbg.bdd.wiremock.scoped.server;

import com.sbg.bdd.wiremock.scoped.admin.model.CorrelationState;

import java.util.Map;

public class ScopeListeners  {

    private Map<String, ScopeListener> scopeListeners;

    public ScopeListeners(Map<String, ScopeListener> scopeListeners) {

        this.scopeListeners = scopeListeners;
    }

    public void fireScopeStarted(CorrelationState knownScope) {
        for (ScopeListener listener : scopeListeners.values()) {
            listener.scopeStarted(knownScope);
        }
    }

    public void fireScopeStopped(CorrelationState state) {
        for (ScopeListener listener : scopeListeners.values()) {
            listener.scopeStopped(state);
        }
    }

    public void fireStepStarted(CorrelationState state) {
        for (ScopeListener listener : scopeListeners.values()) {
            listener.stepStarted(state);
        }

    }

    public void fireStepCompleted(CorrelationState state) {
        for (ScopeListener listener : scopeListeners.values()) {
            listener.stepCompleted(state);
        }
    }
}
