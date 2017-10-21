package com.sbg.bdd.wiremock.scoped.resteasy;


import com.sbg.bdd.wiremock.scoped.cdi.annotations.EndpointInfo;
import com.sbg.bdd.wiremock.scoped.cdi.internal.EndpointInfoLiteral;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.jboss.resteasy.client.ClientRequestFactory;
import org.jboss.resteasy.client.core.executors.ApacheHttpClient4Executor;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;

@ApplicationScoped
public class DynamicClientRequestFactoryProducer {


    public DynamicClientRequestFactoryProducer() {
    }

    @Produces
    @EndpointInfo(propertyName = "")
    public ClientRequestFactory getClientRequestFactory(InjectionPoint ip) {
        EndpointInfo epp = ip.getAnnotated().getAnnotation(EndpointInfo.class);
        return getClientRequestFactory(epp);
    }

    public static ClientRequestFactory newClientRequestFactory(String propertyName, String... categories) {
        return getClientRequestFactory(new EndpointInfoLiteral(propertyName, categories, new String[0]));
    }

    private static ClientRequestFactory getClientRequestFactory(EndpointInfo ep) {
        CloseableHttpClient httpClient = HttpClients.createSystem();
        ApacheHttpClient4Executor clientExecutor = new ApacheHttpClient4Executor(httpClient);
        return new DynamicClientRequestFactory(clientExecutor, ep);
    }
}
