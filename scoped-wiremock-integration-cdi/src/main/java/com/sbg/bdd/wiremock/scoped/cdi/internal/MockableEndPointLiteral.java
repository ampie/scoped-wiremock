package com.sbg.bdd.wiremock.scoped.cdi.internal;


import com.sbg.bdd.wiremock.scoped.cdi.annotations.MockableEndPoint;

import javax.enterprise.util.AnnotationLiteral;

public class MockableEndPointLiteral extends AnnotationLiteral<MockableEndPoint> implements MockableEndPoint {

    private String propertyName;
    private String[] categories;
    private String[] scopes;

    public MockableEndPointLiteral(String propertyName, String[] categories, String[] scopes) {
        this.propertyName = propertyName;
        this.categories = categories;
        this.scopes = scopes;
    }

    @Override
    public String propertyName() {
        return propertyName;
    }

    @Override
    public String[] categories() {
        return categories;
    }

    @Override
    public String[] scopes() {
        return scopes;
    }
}
