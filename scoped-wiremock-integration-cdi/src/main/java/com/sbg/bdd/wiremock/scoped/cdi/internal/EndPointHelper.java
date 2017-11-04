package com.sbg.bdd.wiremock.scoped.cdi.internal;

import javax.xml.ws.BindingProvider;
import java.lang.reflect.Proxy;

public abstract class EndPointHelper {
    public static <T> T wrapEndpoint(T input, String propertyName, String ... categories) {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        EndpointInfoLiteral epp = new EndpointInfoLiteral(propertyName,categories,new String[0]);
        WebServiceHeaderPropagatingHandler ih = new WebServiceHeaderPropagatingHandler((BindingProvider) input, epp);
        return (T) Proxy.newProxyInstance(cl, input.getClass().getInterfaces(), ih);
    }
}
