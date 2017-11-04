package com.sbg.bdd.wiremock.scoped.cdi.internal;

import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.ProxyFactory;
import javassist.util.proxy.ProxyObject;
import org.jboss.security.SecurityContextAssociation;

import javax.ejb.Asynchronous;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class AsyncInvocationHandler implements MethodHandler {
    private Object delegate;

    public AsyncInvocationHandler(Object delegate) {
        this.delegate = delegate;
    }

    public static <T> T create(Field field, Object delegate) throws Exception {
        ProxyFactory factory = new ProxyFactory();
        factory.setSuperclass(field.getType());
        Class clazz = factory.createClass();
        Object instance = clazz.newInstance();
        ((ProxyObject) instance).setHandler(new AsyncInvocationHandler(delegate));
        return (T) instance;
    }
    @Override
    public Object invoke(Object ignoreMeEntirelyAndUseTheDelegate, Method method, Method forwarder, Object[] parameters) throws Throwable {
        if(method.isAnnotationPresent(Asynchronous.class)) {
            RequestScopedWireMockCorrelationState correlationState = (RequestScopedWireMockCorrelationState) SecurityContextAssociation.getSecurityContext().getData().get("correlationState");
            correlationState.newChildContext(method, parameters);
        }
        return method.invoke(this.delegate,parameters);
    }

}
