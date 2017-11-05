package com.sbg.bdd.wiremock.scoped.cdi.internal;

import com.sbg.bdd.wiremock.scoped.integration.BaseWireMockCorrelationState;
import org.jboss.security.SecurityContext;
import org.jboss.security.SecurityContextAssociation;

import javax.enterprise.context.RequestScoped;
import java.util.Map;

@RequestScoped
public class RequestScopedWireMockCorrelationState extends BaseWireMockCorrelationState {
    @Override
    public void set(String correlationPath, int threadContext, boolean proxyUnmappedEndpoints) {
        super.set(correlationPath, threadContext, proxyUnmappedEndpoints);
        SecurityContext securityContext = SecurityContextAssociation.getSecurityContext();
        Map<String, Object> data = securityContext.getData();
        data.put("correlationState", this);
    }
}
