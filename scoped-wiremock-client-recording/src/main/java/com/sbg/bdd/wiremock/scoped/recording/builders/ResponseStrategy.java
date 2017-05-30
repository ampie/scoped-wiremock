package com.sbg.bdd.wiremock.scoped.recording.builders;

import com.sbg.bdd.wiremock.scoped.recording.WireMockContext;

public interface ResponseStrategy {
    ExtendedResponseDefinitionBuilder applyTo(ExtendedMappingBuilder builder, WireMockContext scope) throws Exception;
}
