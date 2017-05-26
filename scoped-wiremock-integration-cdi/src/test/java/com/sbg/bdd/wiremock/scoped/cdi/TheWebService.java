package com.sbg.bdd.wiremock.scoped.cdi;

import javax.jws.WebMethod;

public interface TheWebService {
    @WebMethod
    public void doStoff();
}
