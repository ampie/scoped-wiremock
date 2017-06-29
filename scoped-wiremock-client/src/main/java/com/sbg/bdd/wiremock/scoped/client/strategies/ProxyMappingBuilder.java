package com.sbg.bdd.wiremock.scoped.client.strategies;

import com.sbg.bdd.wiremock.scoped.client.builders.ResponseStrategy;


public class ProxyMappingBuilder {
    String baseUrl;
    int segments;
    String action;
    String which;
    private boolean targetTheServiceUnderTest;

    public ProxyMappingBuilder(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public ProxyMappingBuilder ignoring() {
        action = "ignore";
        return this;
    }

    public ProxyMappingBuilder using() {
        action = "use";
        return this;
    }

    public ProxyMappingBuilder theLast(int number) {
        which = "trailing";
        segments = number;
        return this;
    }

    public ProxyMappingBuilder theFirst(int number) {
        which = "leading";
        segments = number;
        return this;
    }

    public ProxyMappingBuilder theServiceUnderTest() {
        targetTheServiceUnderTest = true;
        return this;
    }

    public ResponseStrategy segments() {
        return ProxyStrategies.target(baseUrl, segments, action, which);
    }

}
