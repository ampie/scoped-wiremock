package com.sbg.bdd.wiremock.scoped.cdi.internal;

import com.sbg.bdd.wiremock.scoped.cdi.annotations.PropagatesHeaders;
import com.sbg.bdd.wiremock.scoped.integration.WireMockCorrelationState;
import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.ProxyFactory;
import javassist.util.proxy.ProxyObject;
import org.jboss.security.SecurityContextAssociation;
import org.jboss.security.SubjectInfo;

import javax.ejb.Asynchronous;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import java.lang.reflect.Method;

@Interceptor
@PropagatesHeaders
public class HeaderPropagatingInterceptor {

    @AroundInvoke
    public Object around(InvocationContext context) throws Exception {
        Object target = context.getTarget();
        if (context.getMethod().isAnnotationPresent(Asynchronous.class)) {
            WireMockCorrelationState correlationState = AsyncInvocationHandler.pullCorrelationState(target, context.getMethod(), context.getParameters());
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
