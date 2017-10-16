package com.sbg.bdd.wiremock.scoped.cdi.internal;

import com.sbg.bdd.wiremock.scoped.integration.DependencyInjectorAdaptor;
import com.sbg.bdd.wiremock.scoped.integration.EndpointRegistry;
import com.sbg.bdd.wiremock.scoped.integration.WireMockCorrelationState;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.naming.InitialContext;

public class CdiAdaptor implements DependencyInjectorAdaptor {
    @Override
    public WireMockCorrelationState getCurrentCorrelationState() {
        return resolveBean(WireMockCorrelationState.class);
    }

    @Override
    public EndpointRegistry getEndpointRegistry() {
        return resolveBean(EndpointRegistry.class);
    }

    private <T> T resolveBean(Class<T> clss) {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        try {
            InitialContext initialContext = new InitialContext();
            BeanManager beanManager = (BeanManager) initialContext.lookup("java:comp/BeanManager");
            Bean<T> bean = (Bean<T>) beanManager.getBeans(clss).iterator().next();
            CreationalContext<T> ctx = beanManager.createCreationalContext(bean);
            return (T) beanManager.getReference(bean, clss, ctx);
        } catch (Exception e) {
            return null;
        }

    }
}
