package com.sbg.bdd.wiremock.scoped.resteasy;


import com.sbg.bdd.wiremock.scoped.cdi.annotations.EndPointProperty;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.jboss.resteasy.client.ClientRequestFactory;
import org.jboss.resteasy.client.core.executors.ApacheHttpClient4Executor;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Inject;

@ApplicationScoped
public class DynamicClientRequestFactoryProducer {


    public DynamicClientRequestFactoryProducer() {
    }

    @Produces
    @EndPointProperty("")
    public ClientRequestFactory getClientRequestFactory(InjectionPoint ip){
        EndPointProperty ep = ip.getAnnotated().getAnnotation(EndPointProperty.class);
        return getClientRequestFactory(ep);
    }

    public ClientRequestFactory getClientRequestFactory(EndPointProperty ep) {
        CloseableHttpClient httpClient = HttpClients.createSystem();
        ApacheHttpClient4Executor clientExecutor = new ApacheHttpClient4Executor(httpClient);
        return new DynamicClientRequestFactory(clientExecutor, ep);
    }
}
