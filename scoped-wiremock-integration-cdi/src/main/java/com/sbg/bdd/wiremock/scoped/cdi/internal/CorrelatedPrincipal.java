package com.sbg.bdd.wiremock.scoped.cdi.internal;

import com.sbg.bdd.wiremock.scoped.integration.RuntimeCorrelationState;

import java.security.Principal;

public interface CorrelatedPrincipal extends Principal{
    RuntimeCorrelationState getCorrelationState();
}
