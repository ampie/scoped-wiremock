package com.sbg.bdd.wiremock.scoped.server;


import com.sbg.bdd.wiremock.scoped.server.junit.ScopedWireMockServerRule;
import org.junit.rules.TestRule;

public class WhenWorkingWithScopes extends com.sbg.bdd.wiremock.scoped.WhenWorkingWithScopes {

    protected TestRule createWireMockRule() {
        return new ScopedWireMockServerRule();
    }

}