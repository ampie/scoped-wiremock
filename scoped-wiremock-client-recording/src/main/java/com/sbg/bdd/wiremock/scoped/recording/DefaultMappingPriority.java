package com.sbg.bdd.wiremock.scoped.recording;

public enum DefaultMappingPriority {
    BODY_KNOWN(3), SPECIFIC_PROXY(4), FALLBACK_PROXY(5);
    int priority;
    private DefaultMappingPriority(int priority){
        this.priority = priority;
    }

    public int priority() {
        return priority;
    }
}
