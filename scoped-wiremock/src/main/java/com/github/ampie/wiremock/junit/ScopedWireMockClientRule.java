package com.github.ampie.wiremock.junit;


import com.github.ampie.wiremock.client.ScopedWireMock;
import com.github.ampie.wiremock.common.Reflection;
import com.github.tomakehurst.wiremock.client.VerificationException;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.verification.NearMiss;
import org.junit.rules.MethodRule;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

import java.util.List;

public class ScopedWireMockClientRule extends ScopedWireMock implements TestRule,MethodRule {
    private boolean failOnUnmatchedStubs=false;

    public ScopedWireMockClientRule(int port, boolean failOnUnmatchedStubs) {
        super(port);
        this.failOnUnmatchedStubs = failOnUnmatchedStubs;
    }

    public ScopedWireMockClientRule(String host, int port, boolean failOnUnmatchedStubs) {
        super(host, port);
        this.failOnUnmatchedStubs = failOnUnmatchedStubs;
    }

    public ScopedWireMockClientRule(String host, int port, String urlPathPrefix, boolean failOnUnmatchedStubs) {
        super(host, port, urlPathPrefix);
        this.failOnUnmatchedStubs = failOnUnmatchedStubs;
    }

    public ScopedWireMockClientRule(String scheme, String host, int port, boolean failOnUnmatchedStubs) {
        super(scheme, host, port);
        this.failOnUnmatchedStubs = failOnUnmatchedStubs;
    }

    public ScopedWireMockClientRule(String scheme, String host, int port, String urlPathPrefix, boolean failOnUnmatchedStubs) {
        super(scheme, host, port, urlPathPrefix);
        this.failOnUnmatchedStubs = failOnUnmatchedStubs;
    }

    @Override
    public Statement apply(final Statement base, Description description) {
        return apply(base, null, null);
    }

    @Override
    public Statement apply(final Statement base, FrameworkMethod method, Object target) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                ThreadLocal<WireMock> defaultInstance = Reflection.getStaticValue(WireMock.class, "defaultInstance");
                defaultInstance.set(ScopedWireMockClientRule.this);
                try {
                    before();
                    base.evaluate();
                    checkForUnmatchedRequests();
                } finally {
                    after();
                    resetAll();
                }
            }

        };
    }

    private void checkForUnmatchedRequests() {
        if (failOnUnmatchedStubs) {
            List<NearMiss> nearMisses = findNearMissesForAllUnmatchedRequests();
            if (!nearMisses.isEmpty()) {
                throw VerificationException.forUnmatchedRequests(nearMisses);
            }
        }
    }

    protected void before() {
        // NOOP
    }

    protected void after() {
        // NOOP
    }

}
