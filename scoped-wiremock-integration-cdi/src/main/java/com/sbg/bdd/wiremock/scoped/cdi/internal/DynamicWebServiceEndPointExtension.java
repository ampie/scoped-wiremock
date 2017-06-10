package com.sbg.bdd.wiremock.scoped.cdi.internal;

import com.sbg.bdd.wiremock.scoped.cdi.annotations.EndPointCategory;
import com.sbg.bdd.wiremock.scoped.cdi.annotations.EndPointProperty;
import com.sbg.bdd.wiremock.scoped.filter.EndpointConfig;
import com.sbg.bdd.wiremock.scoped.filter.EndpointTypeTracker;
import com.sbg.bdd.wiremock.scoped.integration.DependencyInjectionAdaptorFactory;
import com.sbg.bdd.wiremock.scoped.integration.EndPointRegistry;

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
            pit.setInjectionTarget(new InjectionTargetWrapper<>( pit.getInjectionTarget(), webServiceRefs));
        }
    }

    private void registerEndPoints(ProcessInjectionTarget<?> pit) {
        Set<EndPointProperty> webServiceRefs = new HashSet<>();
        for (AnnotatedField<?> annotatedField : pit.getAnnotatedType().getFields()) {
            Field declaredField=annotatedField.getJavaMember();
            String categoryName= EndpointConfig.NO_CATEGORY;
            EndPointCategory category=declaredField.getAnnotation(EndPointCategory.class);
            if(category!=null){
                categoryName=category.value();
            }
            if (isPortRef(declaredField)) {
                EndpointTypeTracker.getInstance().registerSoapEndpoint(declaredField.getAnnotation(EndPointProperty.class).value(),categoryName);
            } else if (declaredField.isAnnotationPresent(EndPointProperty.class)) {
                EndpointTypeTracker.getInstance().registerRestEndpoint(declaredField.getAnnotation(EndPointProperty.class).value(),categoryName);
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
        return declaredField.isAnnotationPresent(WebServiceRef.class) && declaredField.isAnnotationPresent(EndPointProperty.class) && declaredField.getType().isInterface();
    }

}
