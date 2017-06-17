package com.sbg.bdd.wiremock.scoped.cdi.internal;

import com.sbg.bdd.wiremock.scoped.cdi.annotations.EndPointCategory;
import com.sbg.bdd.wiremock.scoped.cdi.annotations.EndPointProperty;

import javax.xml.ws.BindingProvider;
import java.lang.reflect.Proxy;

public class EndPointHelper {
    public static <T> T wrapEndpoint(T input, String propertyName, String category) {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        EndPointProperty epp = new EndPointPropertyLiteral(propertyName);
        EndPointCategory epc = new EndPointCategoryLiteral(category);
        DynamicWebServiceReferenceInvocationHandler ih = new DynamicWebServiceReferenceInvocationHandler((BindingProvider) input, epp, epc);
        return (T) Proxy.newProxyInstance(cl, input.getClass().getInterfaces(), ih);

    }
}
