package com.sbg.bdd.wiremock.scoped.client.junit;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.sbg.bdd.resource.ReadableResource;
import com.sbg.bdd.wiremock.scoped.admin.ScopedAdmin;
import com.sbg.bdd.wiremock.scoped.client.WireMockContext;
import com.sbg.bdd.wiremock.scoped.client.builders.ExtendedMappingBuilder;
import com.sbg.bdd.wiremock.scoped.client.builders.ExtendedRequestPatternBuilder;
import com.sbg.bdd.wiremock.scoped.integration.BaseDependencyInjectorAdaptor;

public class InMemoryWireMockContext implements WireMockContext {
    @Override
    public String getCorrelationPath() {
        return BaseDependencyInjectorAdaptor.CURRENT_CORRELATION_STATE.getCorrelationPath();
    }

    @Override
    public ReadableResource resolveInputResource(String fileName) {
        return null;
    }

    @Override
    public String getBaseUrlOfServiceUnderTest() {
        return null;
    }

    @Override
    public void register(ExtendedMappingBuilder child) {
        child.getRequestPatternBuilder().ensureScopePath(WireMock.equalTo(getCorrelationPath()));
        child.getRequestPatternBuilder().setCorrelationPath(getCorrelationPath());
        ScopedAdmin admin = getScopedAdmin();
        admin.register(child.build());
    }

    private ScopedAdmin getScopedAdmin() {
        return ScopedAdminHolder.getScopedAdmin();
    }

    @Override
    public int count(ExtendedRequestPatternBuilder requestPatternBuilder) {
        return getScopedAdmin().count(requestPatternBuilder.build());
    }

}
