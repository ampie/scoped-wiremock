package com.sbg.bdd.wiremock.scoped.jaxrs;

import com.sbg.bdd.wiremock.scoped.cdi.annotations.EndPointCategory;
import com.sbg.bdd.wiremock.scoped.cdi.annotations.EndPointProperty;

import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;

public class DynamicWebTargetProducer {
    @Produces
    @EndPointProperty("")
    public WebTarget produceIt(InjectionPoint ip) {
        EndPointProperty epp = ip.getAnnotated().getAnnotation(EndPointProperty.class);
        EndPointCategory epc = ip.getAnnotated().getAnnotation(EndPointCategory.class);
        Client client = ClientBuilder.newClient();
        return new DynamicWebTarget(client, epp, epc);
    }
}
