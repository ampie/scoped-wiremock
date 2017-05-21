package com.github.ampie.wiremock.client;

import com.github.ampie.wiremock.junit.ScopedWireMockClientRule;
import com.github.ampie.wiremock.junit.ScopedWireMockServerRule;
import org.junit.Rule;
import org.junit.rules.TestRule;

public class WhenManagingScopedRecordings extends com.github.ampie.wiremock.WhenManagingScopedRecordings {

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
