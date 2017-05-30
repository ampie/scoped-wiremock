package com.sbg.bdd.wiremock.scoped.common;

import java.lang.reflect.InvocationTargetException;


public class ExceptionSafe {

    public static RuntimeException theCauseOf(Throwable e) {
        if (e instanceof InvocationTargetException) {
            return theCauseOf(((InvocationTargetException) e).getTargetException());
        } else if (e instanceof RuntimeException) {
            return (RuntimeException) e;
        } else if (e instanceof Error) {
            //just throw it immediately. No recovery from that
            throw (Error) e;
        } else {
            int i = 0;
            while (i < 10 && e.getCause() != null) {
                if (e.getCause() instanceof RuntimeException) {
                    return (RuntimeException) e.getCause();
                } else {
                    e = e.getCause();
                }
                i++;
            }
            return new RuntimeException(e);
        }
    }
}
