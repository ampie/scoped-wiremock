package com.sbg.bdd.wiremock.scoped.cdi.internal;


import com.sbg.bdd.wiremock.scoped.cdi.annotations.EndPointProperty;
import com.sbg.bdd.wiremock.scoped.integration.EndPointRegistry;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.InjectionTarget;
import javax.xml.ws.BindingProvider;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

class InjectionTargetWrapper<X> implements InjectionTarget<X> {
    private static final Logger LOGGER = Logger.getLogger(InjectionTargetWrapper.class.getName());
    private final InjectionTarget<X> it;
    private final Set<Field> webServiceRefs;
    private final EndPointRegistry endpointRegistry;

    public InjectionTargetWrapper(EndPointRegistry endpointRegistry, InjectionTarget<X> it, Set<Field> webServiceRefs) {
        this.it = it;
        this.webServiceRefs = webServiceRefs;
        this.endpointRegistry = endpointRegistry;
    }

    @Override
    public void inject(X instance, CreationalContext<X> ctx) {
        it.inject(instance, ctx);
        for (Field webServiceRef : webServiceRefs) {
            try {
                Object ref = webServiceRef.get(instance);
                if (ref instanceof BindingProvider) {
                    BindingProvider bp = (BindingProvider) ref;
                    wrapReference(instance, webServiceRef, bp);
                }
            } catch (IllegalAccessException | IllegalArgumentException | IllegalStateException e) {
                LOGGER.log(Level.WARNING, "Could not inject:", e);
            }
        }
    }


    private void wrapReference(X instance, Field webServiceRef, BindingProvider bp) throws IllegalAccessException {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        EndPointProperty epp = webServiceRef.getAnnotation(EndPointProperty.class);
        DynamicWebServiceReferenceInvocationHandler ih = new DynamicWebServiceReferenceInvocationHandler(bp, endpointRegistry, epp);
        webServiceRef.set(instance, Proxy.newProxyInstance(cl, getInterfaces(webServiceRef), ih));
    }


    private Class<?>[] getInterfaces(Field webServiceRef) {
        return new Class<?>[]{BindingProvider.class, webServiceRef.getType()};
    }

    @Override
    public void postConstruct(X instance) {
        it.postConstruct(instance);
    }

    @Override
    public void preDestroy(X instance) {
        it.dispose(instance);
    }


    @Override
    public void dispose(X instance) {
        it.dispose(instance);
    }

    @Override
    public Set<InjectionPoint> getInjectionPoints() {
        return it.getInjectionPoints();
    }

    @Override
    public X produce(CreationalContext<X> ctx) {
        return it.produce(ctx);
    }

}
