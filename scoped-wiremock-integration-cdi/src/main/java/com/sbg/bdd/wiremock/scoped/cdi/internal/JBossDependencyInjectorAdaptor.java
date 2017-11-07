package com.sbg.bdd.wiremock.scoped.cdi.internal;

import com.sbg.bdd.wiremock.scoped.integration.BaseRuntimeCorrelationState;
import com.sbg.bdd.wiremock.scoped.integration.DependencyInjectorAdaptor;
import com.sbg.bdd.wiremock.scoped.integration.EndpointRegistry;
import com.sbg.bdd.wiremock.scoped.integration.RuntimeCorrelationState;
import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.ProxyFactory;
import javassist.util.proxy.ProxyObject;
import org.jboss.security.SecurityContext;
import org.jboss.security.SecurityContextAssociation;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.naming.InitialContext;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.Principal;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class JBossDependencyInjectorAdaptor implements DependencyInjectorAdaptor {
    private static Boolean inWildfly;


    public RuntimeCorrelationState getCurrentCorrelationState() {
        if (inWildfly()) {
            return attachCorrelationStateToPrincipal();
        } else {
            return attachCorrelationStateToContextData();
        }
    }

    //For tests only
    public static void clearInWildfly() {
        inWildfly = null;
    }

    @Override
    public EndpointRegistry getEndpointRegistry() {
        return resolveBean(EndpointRegistry.class);
    }

    private static boolean inWildfly() {
        if (inWildfly == null) {
            for (StackTraceElement stackTraceElement : Thread.currentThread().getStackTrace()) {
                if (stackTraceElement.getClassName().contains("undertow") || stackTraceElement.getClassName().contains("wildfly") || stackTraceElement.getClassName().contains("Wildfly")) {
                    inWildfly = true;
                    break;
                }
            }
            if (inWildfly == null) {
                inWildfly = false;
            }
        }
        return inWildfly;
    }

    private static <T> T resolveBean(Class<T> clss) {
        try {
            InitialContext initialContext = new InitialContext();
            BeanManager beanManager = (BeanManager) initialContext.lookup("java:comp/BeanManager");
            Bean<T> bean = (Bean<T>) beanManager.getBeans(clss).iterator().next();
            CreationalContext<T> ctx = beanManager.createCreationalContext(bean);
            return (T) beanManager.getReference(bean, clss, ctx);
        } catch (Exception e) {
            return null;
        }

    }

    private RuntimeCorrelationState attachCorrelationStateToContextData() {
        //because in JBoss the context data gets propagated
        SecurityContext sc = SecurityContextAssociation.getSecurityContext();
        RuntimeCorrelationState correlationState = (RuntimeCorrelationState) sc.getData().get("correlationState");
        if (correlationState == null) {
            correlationState = new BaseRuntimeCorrelationState();
            sc.getData().put("correlationState", correlationState);
        }
        return correlationState;
    }

    private RuntimeCorrelationState attachCorrelationStateToPrincipal() {
        //because in Wildfly the context data does not propagated, but the principal does
        SecurityContext sc = SecurityContextAssociation.getSecurityContext();
        RuntimeCorrelationState correlationState = null;
        if (!(sc.getUtil().getUserPrincipal() instanceof CorrelatedPrincipal)) {
            correlationState = new BaseRuntimeCorrelationState();
            if (sc.getUtil().getUserPrincipal() == null) {
                sc.getUtil().createSubjectInfo(new AnonymousCorrelatedPrincipal(correlationState), sc.getUtil().getCredential(), sc.getUtil().getSubject());
            } else {
                sc.getUtil().createSubjectInfo(createCorrelatedPrincipalProxy(correlationState, sc.getUtil().getUserPrincipal()), sc.getUtil().getCredential(), sc.getUtil().getSubject());
            }
        } else {
            correlationState = ((CorrelatedPrincipal) sc.getUtil().getUserPrincipal()).getCorrelationState();
        }
        return correlationState;
    }

    private static CorrelatedPrincipal createCorrelatedPrincipalProxy(RuntimeCorrelationState correlationState, final Principal principal) {
        try {
            return createCorrelatedPrincipalProxy(correlationState, principal, principal.getClass(), CorrelatedPrincipal.class);
        } catch (Exception e) {
            //Could be final, not have protected default constructors, etc.
            try {
                return createCorrelatedPrincipalProxy(correlationState, principal, Object.class, findInterfacesToImplement(principal.getClass()));
            } catch (RuntimeException e1) {
                throw e1;
            } catch (Exception e1) {
                throw new IllegalStateException("Could not proxy " + principal.getClass(), e1);
            }
        }
    }

    private static Class<?>[] findInterfacesToImplement(Class<?> clss) {
        Set<Class<?>> result = new HashSet<>();
        result.add(CorrelatedPrincipal.class);
        addInterfaces(clss, result);
        return result.toArray(new Class<?>[result.size()]);
    }

    private static void addInterfaces(Class<?> clss, Set<Class<?>> result) {
        if (!(clss == Object.class || clss == null)) {
            result.addAll(Arrays.asList(clss.getInterfaces()));
            addInterfaces(clss.getSuperclass(), result);
        }
    }

    private static CorrelatedPrincipal createCorrelatedPrincipalProxy(final RuntimeCorrelationState correlationState, final Principal delegate, Class<?> superClass, Class<?>... interfaces) throws InstantiationException, IllegalAccessException, InvocationTargetException {
        ProxyFactory factory = new ProxyFactory();
        factory.setSuperclass(superClass);
        factory.setInterfaces(interfaces);
        Class clazz = factory.createClass();
        Object instance = clazz.newInstance();
        ((ProxyObject) instance).setHandler(new MethodHandler() {
            @Override
            public Object invoke(Object ignoreMeEntirelyUseTheDelegate, Method method, Method someCrappyMethodNameNotToBeUsedEither, Object[] parameters) throws Throwable {
                if (method.getName().equals("getCorrelationState")) {
                    return correlationState;
                } else {
                    return method.invoke(delegate, parameters);
                }
            }
        });
        return (CorrelatedPrincipal) instance;
    }

}
