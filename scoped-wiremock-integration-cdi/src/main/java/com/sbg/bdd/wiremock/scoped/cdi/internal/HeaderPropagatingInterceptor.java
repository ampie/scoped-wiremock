package com.sbg.bdd.wiremock.scoped.cdi.internal;

import com.sbg.bdd.wiremock.scoped.cdi.annotations.PropagatesHeaders;
import org.jboss.security.SecurityContextAssociation;

import javax.ejb.Asynchronous;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

@Interceptor
@PropagatesHeaders
public class HeaderPropagatingInterceptor {

    @AroundInvoke
    public Object around(InvocationContext context) throws Exception {
        if (context.getMethod().isAnnotationPresent(Asynchronous.class)) {
            RequestScopedWireMockCorrelationState correlationState = (RequestScopedWireMockCorrelationState) SecurityContextAssociation.getSecurityContext().getData().get("correlationState");
            if (correlationState != null) {
                correlationState.setCurrentThreadCorrelationContext(context.getMethod(), context.getParameters());
                try {
                    return context.proceed();
                } finally {
                    correlationState.clearCurrentThreadCorrelationContext(context.getMethod(), context.getParameters());
                }
            }
        }
        return context.proceed();
    }
}
