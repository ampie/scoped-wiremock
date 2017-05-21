package com.github.ampie.wiremock.client;

import com.github.ampie.wiremock.junit.ScopedWireMockServerRule;
import com.github.ampie.wiremock.junit.ScopedWireMockClientRule;
import org.junit.Rule;
import org.junit.rules.TestRule;

public class WhenWorkingOutsideOfAnyScope extends com.github.ampie.wiremock.WhenWorkingOutsideOfAnyScope {

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
