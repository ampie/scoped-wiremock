package com.sbg.bdd.wiremock.scoped.cdi.annotations;

import javax.enterprise.util.Nonbinding;
import javax.inject.Qualifier;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Qualifier
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.FIELD})
public @interface EndpointInfo {
    @Nonbinding String propertyName();
    @Nonbinding String[] categories() default {};
    @Nonbinding String[] scopes() default {};
}
