package com.sbg.bdd.wiremock.scoped.cdi.internal;

import com.sbg.bdd.wiremock.scoped.integration.BaseWireMockCorrelationState;
import org.jboss.security.SecurityContextAssociation;

import javax.enterprise.context.RequestScoped;

@RequestScoped
public class RequestScopedWireMockCorrelationState extends BaseWireMockCorrelationState {
    @Override
    public void set(String correlationPath, int threadContext, boolean proxyUnmappedEndpoints) {
        super.set(correlationPath, threadContext, proxyUnmappedEndpoints);
        SecurityContextAssociation.getSecurityContext().getData().put("correlationState", this);
    }
}
