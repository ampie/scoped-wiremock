package com.sbg.bdd.wiremock.scoped.client.builders;

import com.sbg.bdd.wiremock.scoped.client.WireMockContext;

public interface ResponseStrategy {
    ExtendedResponseDefinitionBuilder applyTo(ExtendedMappingBuilder builder, WireMockContext scope) throws Exception;
    String getDescription();
}
