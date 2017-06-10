package com.sbg.bdd.wiremock.scoped.cdi.internal;


import com.sbg.bdd.wiremock.scoped.cdi.annotations.EndPointCategory;
import com.sbg.bdd.wiremock.scoped.cdi.annotations.EndPointProperty;

import javax.enterprise.util.AnnotationLiteral;

public class EndPointCategoryLiteral extends AnnotationLiteral<EndPointCategory> implements EndPointCategory {
    private String name;

    public EndPointCategoryLiteral(String name) {
        this.name = name;
    }

    @Override
    public String value() {
        return name;
    }

}
