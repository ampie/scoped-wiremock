package com.sbg.bdd.wiremock.scoped.cdi;

import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.ProxyFactory;
import javassist.util.proxy.ProxyObject;
import org.jboss.security.SubjectInfo;

import javax.ejb.AsyncResult;
import javax.ejb.Asynchronous;
import java.lang.reflect.Method;
import java.util.concurrent.Future;

public class AsyncBean implements IAsyncBean {
    @Asynchronous
    public Future<String> doStuff() {

        return new AsyncResult<>("stuffs");
    }

}
