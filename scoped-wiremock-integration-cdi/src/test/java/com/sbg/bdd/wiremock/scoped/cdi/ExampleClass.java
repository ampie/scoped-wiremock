package com.sbg.bdd.wiremock.scoped.cdi;

import com.sbg.bdd.wiremock.scoped.cdi.annotations.MockableEndPoint;

import javax.inject.Inject;
import javax.xml.ws.WebServiceRef;

public class ExampleClass {
    @WebServiceRef(value = HelloService.class)
    @MockableEndPoint(propertyName = "my.soap.endpoint.property",
            categories = {"cat1","cat2"},scopes ={"scope1","scope2"})
    public HelloPortType theWebService;
    @Inject
    @MockableEndPoint(propertyName = "my.rest.endpoint.property",
            categories = {"cat1","cat2"},scopes ={"scope1","scope2"})
    public Object theRestService;//We don't care at this point what type it is, just that it has an EndPointProperty
    public ExampleClass() {

    }

    public ExampleClass(HelloPortType theWebService) {
        this.theWebService = theWebService;
    }
}
