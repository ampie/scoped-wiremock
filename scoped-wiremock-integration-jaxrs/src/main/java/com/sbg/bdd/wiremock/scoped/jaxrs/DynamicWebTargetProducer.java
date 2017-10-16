package com.sbg.bdd.wiremock.scoped.jaxrs;

import com.sbg.bdd.wiremock.scoped.cdi.annotations.MockableEndPoint;

import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Inject;
import javax.ws.rs.client.WebTarget;
public class DynamicWebTargetProducer {
    @Inject
    private KeyStoreHelper keystoreHelper;

    @Produces
    @MockableEndPoint(propertyName = "")
    public WebTarget produceIt(InjectionPoint ip) {
        MockableEndPoint epp = ip.getAnnotated().getAnnotation(MockableEndPoint.class);
        return new DynamicWebTarget(keystoreHelper, epp);
    }

}
