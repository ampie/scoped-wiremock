package com.sbg.bdd.wiremock.scoped.cdi.internal;

import com.sbg.bdd.wiremock.scoped.integration.DependencyInjectionAdaptorFactory;
import com.sbg.bdd.wiremock.scoped.integration.RuntimeCorrelationState;
import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.ProxyFactory;
import javassist.util.proxy.ProxyObject;

import javax.ejb.Asynchronous;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class AsyncHeaderPropagatingHandler implements MethodHandler {
    private Object delegate;

    public AsyncHeaderPropagatingHandler(Object delegate) {
        this.delegate = delegate;
    }

    public static <T> T create(Field field, Object delegate) throws Exception {
        ProxyFactory factory = new ProxyFactory();
        factory.setSuperclass(field.getType());
        Class clazz = factory.createClass();
        Object instance = clazz.newInstance();
        ((ProxyObject) instance).setHandler(new AsyncHeaderPropagatingHandler(delegate));
        return (T) instance;
    }

    @Override
    public Object invoke(Object ignoreMeEntirelyAndUseTheDelegate, Method method, Method forwarder, Object[] parameters) throws Throwable {
        if (method.isAnnotationPresent(Asynchronous.class)) {
            RuntimeCorrelationState correlationState = DependencyInjectionAdaptorFactory.getCurrentCorrelationState();
            if (correlationState.isSet()) {
                correlationState.newChildContext(method, parameters);
            }
        }
        return method.invoke(this.delegate, parameters);
    }

}
