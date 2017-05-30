package com.sbg.bdd.wiremock.scoped.client;


import com.sbg.bdd.wiremock.scoped.ScopedWireMockClientRule;
import org.junit.rules.TestRule;

public class WhenWorkingWithScopes extends com.sbg.bdd.wiremock.scoped.WhenWorkingWithScopes {

    protected TestRule createWireMockRule() {
        return new ScopedWireMockClientRule(WireMockServerFactory.createAndReturnPort(),false);
    }
}