package com.sbg.bdd.wiremock.scoped.server;


import com.sbg.bdd.wiremock.scoped.ScopedWireMockTest;
import com.sbg.bdd.wiremock.scoped.server.junit.ScopedWireMockServerRule;
import org.junit.rules.TestRule;

public class WhenPlayingBackResponses extends com.sbg.bdd.wiremock.scoped.WhenPlayingBackResponses {
    protected TestRule createWireMockRule() {
        ScopedWireMockServerRule serverRule = new ScopedWireMockServerRule();
        serverRule.registerResourceRoot("root", ScopedWireMockTest.getDirectoryResourceRoot());
        return serverRule;
    }

}
