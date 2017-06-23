package com.sbg.bdd.wiremock.scoped.cdi.internal;

import com.sbg.bdd.wiremock.scoped.integration.BaseWireMockCorrelationState;

import javax.enterprise.context.RequestScoped;

@RequestScoped
public class RequestScopedWireMockCorrelationState extends BaseWireMockCorrelationState{
}
