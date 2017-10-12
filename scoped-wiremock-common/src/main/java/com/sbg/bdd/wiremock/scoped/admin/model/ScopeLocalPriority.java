package com.sbg.bdd.wiremock.scoped.admin.model;

public enum ScopeLocalPriority {
    JOURNAL(1),//Overrides everything to ensure that playback occurs globally exactly as recorded in a previous run
    RECORDINGS(2),//When not playing back the journal, playing back a specific directory is the most precise way of
    // representing complex sequence of interactions, and we don't want to accidentally overwrite the recorded mappings
    BODY_KNOWN(3),//
    SPECIFIC_PROXY(4),//Proxying is generally more used to save recordings to build more exact mocks from later.
    // This priority allows known bodies to override the proxy rules they evolved from
    FALLBACK_PROXY(5);//These are usually defined earlier in the scope hierarchy for cases that more explicit mocks
    //have not been configured at the lower levels
    int priority;

    private ScopeLocalPriority(int priority) {
        this.priority = priority;
    }

    public int priority() {
        return priority;
    }
}
