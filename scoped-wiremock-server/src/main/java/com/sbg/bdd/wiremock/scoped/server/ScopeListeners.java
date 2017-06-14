package com.sbg.bdd.wiremock.scoped.server;

import com.sbg.bdd.wiremock.scoped.admin.ScopedAdmin;
import com.sbg.bdd.wiremock.scoped.admin.model.CorrelationState;

import java.util.Map;

public class ScopeListeners  {

    private ScopedAdmin admin;
    private Map<String, ScopeListener> scopeListeners;

    public ScopeListeners(ScopedAdmin admin, Map<String, ScopeListener> scopeListeners) {
        this.admin = admin;

        this.scopeListeners = scopeListeners;
    }

    public void fireScopeStarted(CorrelationState knownScope) {
        for (ScopeListener listener : scopeListeners.values()) {
            listener.setScopedAdmin(admin);
            listener.scopeStarted(knownScope);
        }
    }

    public void fireScopeStopped(CorrelationState state) {
        for (ScopeListener listener : scopeListeners.values()) {
            listener.setScopedAdmin(admin);
            listener.scopeStopped(state);
        }
    }

    public void fireStepStarted(CorrelationState state) {
        for (ScopeListener listener : scopeListeners.values()) {
            listener.setScopedAdmin(admin);
            listener.stepStarted(state);
        }

    }

    public void fireStepCompleted(CorrelationState state) {
        for (ScopeListener listener : scopeListeners.values()) {
            listener.setScopedAdmin(admin);
            listener.stepCompleted(state);
        }
    }
}
