package com.sbg.bdd.wiremock.scoped.recording;


import com.sbg.bdd.resource.ReadableResource;
import com.sbg.bdd.wiremock.scoped.recording.builders.ExtendedMappingBuilder;
import com.sbg.bdd.wiremock.scoped.recording.builders.ExtendedRequestPatternBuilder;
import com.sbg.bdd.wiremock.scoped.recording.endpointconfig.EndpointConfigRegistry;


public interface WireMockContext extends EndpointConfigRegistry {

    ReadableResource resolveInputResource(String fileName);

    String getBaseUrlOfServiceUnderTest();

    void register(ExtendedMappingBuilder child);

    int count(ExtendedRequestPatternBuilder requestPatternBuilder);

    Integer calculatePriority(int localLevel);
}
