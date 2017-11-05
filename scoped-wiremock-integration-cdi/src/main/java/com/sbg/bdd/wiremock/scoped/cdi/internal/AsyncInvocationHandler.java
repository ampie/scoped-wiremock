package com.sbg.bdd.wiremock.scoped.cdi.internal;

import com.sbg.bdd.wiremock.scoped.integration.WireMockCorrelationState;
import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.ProxyFactory;
import javassist.util.proxy.ProxyObject;
import org.jboss.security.SecurityContext;
import org.jboss.security.SecurityContextAssociation;
import org.jboss.security.SubjectInfo;
import org.jboss.security.identity.Identity;

import javax.ejb.Asynchronous;
import javax.security.auth.Subject;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.security.Principal;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class AsyncInvocationHandler implements MethodHandler {
    private static Boolean inWildfly;
    //OUCH!!!!!Wildfly is a bitch. Low memory requirements and its state is cleared from the request, so its fairly low risk
    //Wildfly seems to create a new instance of a request scoped bean from every new thread.
    private static ThreadLocal<RequestScopedWireMockCorrelationState> correlationState = new ThreadLocal<>();

    public static void setCorrelationStateForTests(RequestScopedWireMockCorrelationState correlationState) {
        AsyncInvocationHandler.correlationState.set(correlationState);
    }

    public static RequestScopedWireMockCorrelationState getCurrentCorrelationState() {
        if (inWildfly()) {
            SecurityContext sc = SecurityContextAssociation.getSecurityContext();
            RequestScopedWireMockCorrelationState correlationState=null;
            if (!(sc.getUtil().getUserPrincipal() instanceof MyPrincipal)) {
                correlationState = new RequestScopedWireMockCorrelationState();
                sc.getUtil().createSubjectInfo(new MyPrincipal(correlationState), sc.getUtil().getCredential(), sc.getUtil().getSubject());
            } else {
                correlationState=((MyPrincipal)sc.getUtil().getUserPrincipal()).correlationState;
            }

//            if (AsyncInvocationHandler.correlationState.get() == null) {
//                AsyncInvocationHandler.correlationState.set(new RequestScopedWireMockCorrelationState());
//            }
//            return correlationState.get();
            return correlationState;
        } else {
            SecurityContext sc = SecurityContextAssociation.getSecurityContext();
            RequestScopedWireMockCorrelationState correlationState = (RequestScopedWireMockCorrelationState) sc.getData().get("correlationState");
            if (correlationState == null) {
                    correlationState = CdiAdaptor.resolveBean(RequestScopedWireMockCorrelationState.class);
                sc.getData().put("correlationState", correlationState);
                if (correlationState == null) {
                    return AsyncInvocationHandler.correlationState.get();//for tests
                }
            }
            return correlationState;
        }
    }

    private static class MyPrincipal implements Principal {
        RequestScopedWireMockCorrelationState correlationState;

        public MyPrincipal(RequestScopedWireMockCorrelationState correlationState) {
            this.correlationState = correlationState;
        }

        @Override
        public String getName() {
            return "ekke";
        }
    }


    private static class Key {
        Object target;
        Method method;
        Object[] parameters;

        public Key(Object target, Method method, Object[] parameters) {
            this.target = target;
            this.method = method;
            this.parameters = parameters;
        }

        @Override
        public int hashCode() {
            return method.getName().hashCode();
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof Key) {
                Key other = (Key) o;
                if (other.method.getName().equals(method.getName()) && other.parameters.length == parameters.length) {
                    for (int i = 0; i < other.parameters.length; i++) {
                        if (other.parameters[i] != parameters[i]) {
                            return false;
                        }
                    }
                    return true;
                }
            }
            return false;

        }
    }

    private Object delegate;
    private static ConcurrentMap<Key, RequestScopedWireMockCorrelationState> map = new ConcurrentHashMap<>();


    public AsyncInvocationHandler(Object delegate) {
        this.delegate = delegate;
    }

    public static <T> T create(Field field, Object delegate) throws Exception {
        ProxyFactory factory = new ProxyFactory();
        factory.setSuperclass(field.getType());
        Class clazz = factory.createClass();
        Object instance = clazz.newInstance();
        ((ProxyObject) instance).setHandler(new AsyncInvocationHandler(delegate));
        return (T) instance;
    }

    @Override
    public Object invoke(Object ignoreMeEntirelyAndUseTheDelegate, Method method, Method forwarder, Object[] parameters) throws Throwable {
        if (method.isAnnotationPresent(Asynchronous.class)) {
            RequestScopedWireMockCorrelationState correlationState = getCurrentCorrelationState();
            if (inWildfly()) {
                //TODO only do this in Wildfly and then find a better solution for Wildfly. This will have a significant performance impact.
                pushCorrelationState(delegate, method, parameters, correlationState);
            }
            if (correlationState.isSet()) {
                correlationState.newChildContext(method, parameters);
            }
        }
        return method.invoke(this.delegate, parameters);
    }

    private static boolean inWildfly() {
        if (inWildfly == null) {
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            for (StackTraceElement stackTraceElement : stackTrace) {
                if (stackTraceElement.getClassName().contains("undertow") || stackTraceElement.getClassName().contains("wildfly")) {
                    inWildfly = true;
                }
            }
            if (inWildfly == null) {
                inWildfly = false;
            }
        }
        return inWildfly;
    }

    private static void pushCorrelationState(Object delegate, Method method, Object[] parameters, RequestScopedWireMockCorrelationState correlationState) {
//        map.put(new Key(delegate, method, parameters), correlationState);
    }

    public static WireMockCorrelationState pullCorrelationState(Object target, Method method, Object[] parameters) {
        if (inWildfly()) {
            return ((MyPrincipal)SecurityContextAssociation.getSecurityContext().getUtil().getUserPrincipal()).correlationState;
//            correlationState.set(map.remove(new Key(target, method, parameters)));
//            return correlationState.get();
        } else {
            return (WireMockCorrelationState) SecurityContextAssociation.getSecurityContext().getData().get("correlationState");
        }
    }
}
