package com.sbg.bdd.wiremock.scoped.integration;

import java.util.ServiceLoader;


public class DependencyInjectionAdaptorFactory {

    private static DependencyInjectorAdaptor adaptor;

    //For tests
    public static void useAdapter(DependencyInjectorAdaptor a) {
        adaptor = a;
    }

    public static WireMockCorrelationState getCurrentCorrelationState() {
        return getAdaptor().getCurrentCorrelationState();
    }

    public static DependencyInjectorAdaptor getAdaptor() {
        if (DependencyInjectionAdaptorFactory.adaptor == null) {
            for (DependencyInjectorAdaptor helper : ServiceLoader.load(DependencyInjectorAdaptor.class)) {
                DependencyInjectionAdaptorFactory.adaptor = helper;
            }
        }
        if (DependencyInjectionAdaptorFactory.adaptor == null) {
            throw new IllegalStateException("No " + DependencyInjectorAdaptor.class.getName() + " registered");
        }
        return DependencyInjectionAdaptorFactory.adaptor;
    }
}
