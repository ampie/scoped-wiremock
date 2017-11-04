package com.sbg.bdd.wiremock.scoped.cdi;

import com.sbg.bdd.wiremock.scoped.cdi.annotations.EndpointInfo;

import javax.inject.Inject;
import javax.xml.ws.WebServiceRef;

public class ExampleClass {
    @WebServiceRef(value = HelloService.class)
    @EndpointInfo(propertyName = "my.soap.endpoint.property",
            categories = {"cat1","cat2"},scopes ={"scope1","scope2"})
    public HelloPortType theWebService;
    @Inject
    @EndpointInfo(propertyName = "my.rest.endpoint.property",
            categories = {"cat1","cat2"},scopes ={"scope1","scope2"})
    public Object theRestService;//We don't care at this point what type it is, just that it has an EndPointProperty
    @Inject
    public AsyncBean asyncBean=new AsyncBean();
    public ExampleClass() {

    }

    public ExampleClass(HelloPortType theWebService) {
        this.theWebService = theWebService;
    }
}
