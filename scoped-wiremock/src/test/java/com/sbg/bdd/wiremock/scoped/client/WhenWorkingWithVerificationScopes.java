package com.sbg.bdd.wiremock.scoped.client;


import com.sbg.bdd.wiremock.scoped.junit.ScopedWireMockClientRule;
import com.sbg.bdd.wiremock.scoped.junit.ScopedWireMockServerRule;
import org.junit.Rule;
import org.junit.rules.TestRule;

public class WhenWorkingWithVerificationScopes extends com.sbg.bdd.wiremock.scoped.WhenWorkingWithVerificationScopes {

    @Rule
    public ScopedWireMockServerRule serverRule;
    protected TestRule createWireMockRule() {
        serverRule = new ScopedWireMockServerRule();
        return new ScopedWireMockClientRule(serverRule.port(),false){
            @Override
            public void resetAll() {

            }
        };
    }
}