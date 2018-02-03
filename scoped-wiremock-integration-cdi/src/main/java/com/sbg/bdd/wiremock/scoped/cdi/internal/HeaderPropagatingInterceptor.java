package com.sbg.bdd.wiremock.scoped.cdi.internal;

import com.sbg.domain.common.annotations.PropagatesHeaders;
import com.sbg.bdd.wiremock.scoped.integration.DependencyInjectionAdaptorFactory;
import com.sbg.bdd.wiremock.scoped.integration.RuntimeCorrelationState;

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
            RuntimeCorrelationState correlationState = DependencyInjectionAdaptorFactory.getCurrentCorrelationState();
            if (correlationState != null && correlationState.isSet()) {
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
