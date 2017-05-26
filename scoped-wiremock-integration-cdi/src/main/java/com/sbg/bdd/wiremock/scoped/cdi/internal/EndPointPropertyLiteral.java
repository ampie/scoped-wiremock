package com.sbg.bdd.wiremock.scoped.cdi.internal;


import com.sbg.bdd.wiremock.scoped.cdi.annotations.EndPointProperty;

import javax.enterprise.util.AnnotationLiteral;

public class EndPointPropertyLiteral extends AnnotationLiteral<EndPointProperty> implements EndPointProperty {
    private String name;

    public EndPointPropertyLiteral(String name) {
        this.name = name;
    }

    @Override
    public String value() {
        return name;
    }

}
