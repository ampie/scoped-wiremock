package com.sbg.bdd.wiremock.scoped.cdi.internal;

import com.sbg.bdd.wiremock.scoped.integration.DependencyInjectorAdaptor;
import com.sbg.bdd.wiremock.scoped.integration.EndPointRegistry;
import com.sbg.bdd.wiremock.scoped.integration.WireMockCorrelationState;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.naming.InitialContext;

public class CdiAdaptor implements DependencyInjectorAdaptor {
    @Override
    public WireMockCorrelationState getCurrentCorrelationState() {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        WireMockCorrelationState pr = null;
        try {
            InitialContext initialContext = new InitialContext();
            BeanManager beanManager = (BeanManager) initialContext.lookup("java:comp/BeanManager");
            Bean<WireMockCorrelationState> bean = (Bean<WireMockCorrelationState>) beanManager.getBeans(WireMockCorrelationState.class).iterator().next();
            CreationalContext<WireMockCorrelationState> ctx = beanManager.createCreationalContext(bean);
            return (WireMockCorrelationState) beanManager.getReference(bean, WireMockCorrelationState.class, ctx);
        } catch (Exception e) {
            return null;
        }

    }

    @Override
    public EndPointRegistry getEndpointRegistry() {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        try {
            InitialContext initialContext = new InitialContext();
            BeanManager beanManager = (BeanManager) initialContext.lookup("java:comp/BeanManager");
            Bean<EndPointRegistry> bean = (Bean<EndPointRegistry>) beanManager.getBeans(EndPointRegistry.class).iterator().next();
            CreationalContext<EndPointRegistry> ctx = beanManager.createCreationalContext(bean);
            return (EndPointRegistry) beanManager.getReference(bean, EndPointRegistry.class, ctx);
        } catch (Exception e) {
            return null;
        }

    }
}
