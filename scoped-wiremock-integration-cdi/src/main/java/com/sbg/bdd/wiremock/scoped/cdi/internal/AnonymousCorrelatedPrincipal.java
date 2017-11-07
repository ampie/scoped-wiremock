package com.sbg.bdd.wiremock.scoped.cdi.internal;

import com.sbg.bdd.wiremock.scoped.integration.RuntimeCorrelationState;

public class AnonymousCorrelatedPrincipal implements CorrelatedPrincipal {
    private RuntimeCorrelationState correlationState;

    public AnonymousCorrelatedPrincipal(RuntimeCorrelationState correlationState) {
        this.correlationState = correlationState;
    }

    @Override
    public RuntimeCorrelationState getCorrelationState() {
        return this.correlationState;
    }

    @Override
    public String getName() {
        return "anonymous";
    }

    @Override
    public int hashCode() {
        return getName().hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof AnonymousCorrelatedPrincipal && (this == o || o.toString().equals(this.toString()));
    }
}
