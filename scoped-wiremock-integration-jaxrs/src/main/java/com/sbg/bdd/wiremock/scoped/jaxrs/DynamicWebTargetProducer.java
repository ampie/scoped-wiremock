package com.sbg.bdd.wiremock.scoped.jaxrs;

import com.sbg.bdd.wiremock.scoped.cdi.annotations.EndPointCategory;
import com.sbg.bdd.wiremock.scoped.cdi.annotations.EndPointProperty;
import org.apache.http.conn.ssl.AllowAllHostnameVerifier;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Inject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
public class DynamicWebTargetProducer {
    @Inject
    private KeyStoreHelper keystoreHelper;

    @Produces
    @EndPointProperty("")
    public WebTarget produceIt(InjectionPoint ip) {
        EndPointProperty epp = ip.getAnnotated().getAnnotation(EndPointProperty.class);
        EndPointCategory epc = ip.getAnnotated().getAnnotation(EndPointCategory.class);
        return new DynamicWebTarget(keystoreHelper, epp, epc);
    }

}
