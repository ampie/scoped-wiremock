package com.sbg.bdd.wiremock.scoped.cdi.internal;

import com.sbg.bdd.wiremock.scoped.cdi.annotations.EndpointInfo;
import com.sbg.bdd.wiremock.scoped.filter.ServerSideEndPointConfigRegistry;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessInjectionTarget;
import javax.xml.ws.WebServiceRef;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;


public class DynamicWebServiceEndPointExtension implements Extension {

    public <X> void processInjectionTarget(@Observes ProcessInjectionTarget<X> pit) {
        registerEndPoints(pit);
        Set<Field> webServiceRefs = extractWebServiceReferences(pit);
        if (webServiceRefs.size() > 0) {
            pit.setInjectionTarget(new InjectionTargetWrapper<>(pit.getInjectionTarget(), webServiceRefs));
        }
    }

    private void registerEndPoints(ProcessInjectionTarget<?> pit) {
        Set<EndpointInfo> webServiceRefs = new HashSet<>();
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

    private <X> boolean isPortRef(Field declaredField) {
        return declaredField.isAnnotationPresent(WebServiceRef.class) && declaredField.isAnnotationPresent(EndpointInfo.class) && declaredField.getType().isInterface();
    }

}
