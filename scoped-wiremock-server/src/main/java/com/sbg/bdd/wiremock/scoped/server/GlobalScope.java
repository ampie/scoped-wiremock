package com.sbg.bdd.wiremock.scoped.server;

import com.sbg.bdd.wiremock.scoped.admin.endpointconfig.RemoteEndPointConfigRegistry;
import com.sbg.bdd.wiremock.scoped.admin.model.CorrelationState;

import java.net.URL;

public class GlobalScope extends CorrelatedScope {
    private String testRunName;
    private URL wireMockPublicUrl;
    private int sequenceNumber;
    private URL baseurlOfServiceUnderTest;
    private RemoteEndPointConfigRegistry endPointConfigRegistry;

    public GlobalScope(String testRunName, URL wireMockPublicUrl, URL baseurlOfServiceUnderTest, int sequenceNumber) {
        super(null, testRunName, new CorrelationState(toKey(testRunName, wireMockPublicUrl, sequenceNumber)));
        this.testRunName = testRunName;
        this.wireMockPublicUrl = wireMockPublicUrl;
        this.sequenceNumber = sequenceNumber;
        this.baseurlOfServiceUnderTest = baseurlOfServiceUnderTest;
        this.endPointConfigRegistry = new RemoteEndPointConfigRegistry(baseurlOfServiceUnderTest.toExternalForm(), testRunName);
    }

    public static String toKey(String testRunName, URL wireMockPublicUrl, int sequenceNumber) {
        return wireMockPublicUrl.getHost() + "/" + wireMockPublicUrl.getPort() + "/" + testRunName + "/" + sequenceNumber;
    }


    public String getKey() {
        return getCorrelationState().getCorrelationPath();
    }

    public CorrelatedScope findOrCreateNestedScope(String correlationPath) {
        String[] split = correlationPath.split("\\/");
        CorrelatedScope previousScope = this;
        StringBuilder currentPath = new StringBuilder();
        for (int i = 0; i < split.length; i++) {
            currentPath.append(split[i]);
            if (i >= 4) {
                if (previousScope.getChild(split[i]) == null) {
                    previousScope.addChild(new CorrelatedScope(previousScope, split[i], new CorrelationState(currentPath.toString())));
                }
                previousScope = previousScope.getChild(split[i]);
            }
            currentPath.append('/');
        }
        return previousScope;
    }

    public CorrelatedScope findNestedScope(String correlationPath) {
        String[] split = correlationPath.split("\\/");
        CorrelatedScope previousScope = this;
        StringBuilder currentPath = new StringBuilder();
        for (int i = 0; i < split.length; i++) {
            currentPath.append(split[i]);
            if (i >= 4) {
                if (previousScope.getChild(split[i]) == null) {
                    return null;
                } else {
                    previousScope = previousScope.getChild(split[i]);
                }
            }
            currentPath.append('/');
        }
        return previousScope;
    }
    public GlobalScope getGlobalScope() {
        return this;
    }
    public int getLevel() {
        return 0;
    }

    public RemoteEndPointConfigRegistry getEndPointConfigRegistry() {
        return endPointConfigRegistry;
    }
}
