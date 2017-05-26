package com.sbg.bdd.wiremock.scoped.extended;

import com.sbg.bdd.wiremock.scoped.WireMockContext;

public interface ResponseStrategy {
    ExtendedResponseDefinitionBuilder applyTo(ExtendedMappingBuilder builder, WireMockContext scope) throws Exception;
}
