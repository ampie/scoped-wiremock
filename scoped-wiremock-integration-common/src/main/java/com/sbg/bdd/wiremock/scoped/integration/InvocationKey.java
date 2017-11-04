package com.sbg.bdd.wiremock.scoped.integration;

import java.lang.reflect.Method;

/**
 * NB!! Assumption is that the calling context has the same instances for parameters as the receiving context.
 * Theoretically This may not always be the case where CDI beans are passed as parameters, but it is an acceptable
 * risk at this point.
 */
public class InvocationKey {
    private Method method;
    private Object[] parameters;

    public InvocationKey(Method method, Object[] parameters) {
        this.method = method;
        this.parameters = parameters;
    }

    @Override
    public int hashCode() {
        return method.getName().hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if(o instanceof InvocationKey){
            InvocationKey other = (InvocationKey) o;
            if(other.parameters.length==parameters.length && other.method.getName().equals(method.getName())){
                for (int i = 0; i < parameters.length; i++) {
                    if(other.parameters[i]!=parameters[i]){
                        //Quite aggressive, but we need it fast
                        return false;
                    }
                }
                return true;
            }
        }
        return false;
    }
}
