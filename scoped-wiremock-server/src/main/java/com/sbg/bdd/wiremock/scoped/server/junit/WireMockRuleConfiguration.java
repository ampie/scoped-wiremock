package com.sbg.bdd.wiremock.scoped.server.junit;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;

public class WireMockRuleConfiguration extends WireMockConfiguration {
    private boolean verbose;
    private boolean failOnUnmatchedStubs;
    private boolean failOnUnusedMappings;
    
    public static WireMockRuleConfiguration wireMockConfig() {
        return new WireMockRuleConfiguration();
    }
    public WireMockRuleConfiguration verbose(){
        this.verbose = true;
        return this;
    }

    public WireMockRuleConfiguration failOnUnmatchedStubs(){
        this.failOnUnmatchedStubs = true;
        return this;
    }
    public WireMockRuleConfiguration failOnUnusedMappings(){
        this.failOnUnusedMappings = true;
        return this;
    }

    @Override
    public WireMockRuleConfiguration port(int portNumber) {
        return (WireMockRuleConfiguration)super.port(portNumber);
    }

    public boolean shouldFailOnUnmatchedStubs() {
        return this.failOnUnmatchedStubs;
    }
}
