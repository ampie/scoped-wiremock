package com.sbg.bdd.wiremock.scoped.cdi;

import javax.ejb.AsyncResult;
import javax.ejb.Asynchronous;
import java.util.concurrent.Future;

public class AsyncBean {
    @Asynchronous
    public Future<String> doStuff(){
        return new AsyncResult<>("stuffs");
    }
}
