package com.sbg.bdd.wiremock.scoped;

import com.sbg.bdd.wiremock.scoped.integration.EndPointRegistry;
import com.sbg.bdd.wiremock.scoped.extended.ExtendedMappingBuilder;
import com.sbg.bdd.wiremock.scoped.extended.ExtendedRequestPatternBuilder;

import java.io.File;


public interface WireMockContext extends EndPointRegistry {

    File resolveResource(String fileName);

    String getBaseUrlOfServiceUnderTest();

    void register(ExtendedMappingBuilder child);

    int count(ExtendedRequestPatternBuilder requestPatternBuilder);

    Integer calculatePriority(int localLevel);
}
