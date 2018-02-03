package com.sbg.bdd.wiremock.scoped.jaxrs;

import com.sbg.domain.common.annotations.EndpointInfo;

import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Inject;
import javax.ws.rs.client.WebTarget;
public class DynamicWebTargetProducer {
    @Inject
    private KeyStoreHelper keystoreHelper;

    @Produces
    @EndpointInfo(propertyName = "")
    public WebTarget produceIt(InjectionPoint ip) {
        EndpointInfo epp = ip.getAnnotated().getAnnotation(EndpointInfo.class);
        return new DynamicWebTarget(keystoreHelper, epp);
    }

}
