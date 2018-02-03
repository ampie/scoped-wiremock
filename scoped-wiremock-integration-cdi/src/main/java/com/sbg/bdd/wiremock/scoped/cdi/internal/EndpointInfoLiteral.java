package com.sbg.bdd.wiremock.scoped.cdi.internal;


import com.sbg.domain.common.annotations.EndpointInfo;

import javax.enterprise.util.AnnotationLiteral;

public class EndpointInfoLiteral extends AnnotationLiteral<EndpointInfo> implements EndpointInfo {

    private String propertyName;
    private String[] categories;
    private String[] scopes;

    public EndpointInfoLiteral(String propertyName, String[] categories, String[] scopes) {
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
