package com.sbg.bdd.wiremock.scoped.cdi;

import com.sbg.bdd.wiremock.scoped.cdi.annotations.EndPointProperty;

import javax.xml.ws.WebServiceRef;

public class ExampleClass {
    @WebServiceRef
    @EndPointProperty("my.endpoint.property")
    public TheWebService theWebService;
    public ExampleClass() {

    }

    public ExampleClass(TheWebService theWebService) {
        this.theWebService = theWebService;
    }
}
