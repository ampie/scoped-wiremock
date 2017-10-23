package com.sbg.bdd.wiremock.scoped.server;

import com.sbg.bdd.wiremock.scoped.admin.model.CorrelationState;

public class AbstractCorrelatedScope {
    protected CorrelatedScope parent;
    protected String name;
    protected CorrelationState correlationState;

    public AbstractCorrelatedScope(CorrelatedScope parent, String name, CorrelationState correlationState) {
        this.parent = parent;
        this.name = name;
        this.correlationState=correlationState;
    }

    public int getLevel() {
        return parent.getLevel() + 1;
    }
    public CorrelatedScope getParent() {
        return parent;
    }

    public String getName() {
        return name;
    }

    public CorrelationState getCorrelationState() {
        return correlationState;
    }

    public String getCorrelationPath() {
        return correlationState.getCorrelationPath();
    }

    public GlobalScope getGlobalScope() {
        return getParent().getGlobalScope();
    }
}
