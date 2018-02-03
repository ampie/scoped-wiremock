package com.sbg.bdd.wiremock.scoped.cdi.internal;

import com.sbg.domain.common.annotations.EndpointInfo;
import com.sbg.domain.common.annotations.PropagatesHeaders;
import com.sbg.bdd.wiremock.scoped.filter.ServerSideEndPointConfigRegistry;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessInjectionTarget;
import javax.xml.ws.WebServiceRef;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Future;


public class HeaderPropagatingExtension implements Extension {

    public <X> void processInjectionTarget(@Observes ProcessInjectionTarget<X> pit) {
        registerEndPoints(pit);
        Set<Field> webServiceRefs = extractWebServiceReferences(pit);
        Set<Field> headerPropagators= extractHeaderPropagators(pit);
        if (webServiceRefs.size() > 0 || headerPropagators.size()>0) {
            pit.setInjectionTarget(new HeaderPropagatingInjectionTargetDecorator<>(pit.getInjectionTarget(), webServiceRefs,headerPropagators));
        }
    }

    private void registerEndPoints(ProcessInjectionTarget<?> pit) {
        for (AnnotatedField<?> annotatedField : pit.getAnnotatedType().getFields()) {
            Field declaredField = annotatedField.getJavaMember();
            if (isPortRef(declaredField)) {
                EndpointInfo annotation = declaredField.getAnnotation(EndpointInfo.class);
                ServerSideEndPointConfigRegistry.getInstance().registerSoapEndpoint(annotation.propertyName(), annotation.categories(), annotation.scopes());
            } else if (declaredField.isAnnotationPresent(EndpointInfo.class)) {
                EndpointInfo annotation = declaredField.getAnnotation(EndpointInfo.class);
                ServerSideEndPointConfigRegistry.getInstance().registerRestEndpoint(annotation.propertyName(), annotation.categories(), annotation.scopes());
            }
        }
    }

    private Set<Field> extractHeaderPropagators(ProcessInjectionTarget<?> pit) {
        Set<Field> headerPropagators = new HashSet<>();
        for (AnnotatedField<?> declaredField : pit.getAnnotatedType().getFields()) {
            if (isHeaderPropagator(declaredField.getJavaMember())) {
                declaredField.getJavaMember().setAccessible(true);
                headerPropagators.add(declaredField.getJavaMember());
            }
        }
        return headerPropagators;
    }

    private boolean isHeaderPropagator(Field declaredField) {
        Class<?> type = declaredField.getType();
        if(isHeaderPropagator(type)){
            for (Method method : type.getMethods()) {
                if(Future.class.isAssignableFrom(method.getReturnType())){
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isHeaderPropagator(Class<?> type) {
        if(type==Object.class || type == null){
            //Dunno why but somehow only on Wildfly do we get a null here sometimes
            return false;
        }
        if(type.isAnnotationPresent(PropagatesHeaders.class)){
            return true;
        }
        Class<?>[] interfaces = type.getInterfaces();
        for (Class<?> anInterface : interfaces) {
            if(isHeaderPropagator(anInterface)){
                return true;
            }
        }
        if(!type.isInterface()){
            return isHeaderPropagator(type.getSuperclass());
        }else {
            return false;
        }
    }

    private Set<Field> extractWebServiceReferences(ProcessInjectionTarget<?> pit) {
        Set<Field> webServiceRefs = new HashSet<>();
        for (AnnotatedField<?> declaredField : pit.getAnnotatedType().getFields()) {
            if (isPortRef(declaredField.getJavaMember())) {
                declaredField.getJavaMember().setAccessible(true);
                webServiceRefs.add(declaredField.getJavaMember());
            }
        }
        return webServiceRefs;
    }

    private boolean isPortRef(Field declaredField) {
        return declaredField.isAnnotationPresent(WebServiceRef.class) && declaredField.isAnnotationPresent(EndpointInfo.class) && declaredField.getType().isInterface();
    }

}
