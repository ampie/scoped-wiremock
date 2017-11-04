package com.sbg.bdd.wiremock.scoped.integration;

import java.util.concurrent.atomic.AtomicInteger;

public class ThreadCorrelationContext {
    public ThreadCorrelationContext parent;
    public AtomicInteger childCount=new AtomicInteger(0);
    public int id;

    public ThreadCorrelationContext(ThreadCorrelationContext parent) {
        this(parent.childCount.incrementAndGet());
        this.parent = parent;
    }

    public ThreadCorrelationContext() {
        this.id = 1;
    }

    public ThreadCorrelationContext(int id) {
        this.id = id;
    }

    public int getContextId() {
        if (parent == null) {
            return this.id;
        } else {
            return (parent.getContextId() * 100) + id;
        }
    }

}
