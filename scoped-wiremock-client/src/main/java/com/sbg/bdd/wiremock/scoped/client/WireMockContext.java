package com.sbg.bdd.wiremock.scoped.client;


import com.sbg.bdd.resource.ReadableResource;
import com.sbg.bdd.wiremock.scoped.client.builders.ExtendedMappingBuilder;
import com.sbg.bdd.wiremock.scoped.client.builders.ExtendedRequestPatternBuilder;


public interface WireMockContext {
    String getCorrelationPath();
    
    ReadableResource resolveInputResource(String fileName);

    String getBaseUrlOfServiceUnderTest();

    void register(ExtendedMappingBuilder child);

    int count(ExtendedRequestPatternBuilder requestPatternBuilder);

}
