package com.sbg.bdd.wiremock.scoped.resteasy;


import com.sbg.bdd.wiremock.scoped.cdi.annotations.EndPointCategory;
import com.sbg.bdd.wiremock.scoped.cdi.annotations.EndPointProperty;
import com.sbg.bdd.wiremock.scoped.cdi.internal.EndPointCategoryLiteral;
import com.sbg.bdd.wiremock.scoped.cdi.internal.EndPointPropertyLiteral;
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
        EndPointProperty epp = ip.getAnnotated().getAnnotation(EndPointProperty.class);
        EndPointCategory epc = ip.getAnnotated().getAnnotation(EndPointCategory.class);
        return getClientRequestFactory(epp,epc);
    }
    public static ClientRequestFactory newClientRequestFactory(String propertyName, String category){
        return getClientRequestFactory(new EndPointPropertyLiteral(propertyName),new EndPointCategoryLiteral(category));
    }
    private static ClientRequestFactory getClientRequestFactory(EndPointProperty ep, EndPointCategory epc) {
        CloseableHttpClient httpClient = HttpClients.createSystem();
        ApacheHttpClient4Executor clientExecutor = new ApacheHttpClient4Executor(httpClient);
        return new DynamicClientRequestFactory(clientExecutor, ep,epc);
    }
}
