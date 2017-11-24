package com.sbg.bdd.wiremock.scoped.server.junit;


import com.github.tomakehurst.wiremock.client.VerificationException;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.Options;
import com.github.tomakehurst.wiremock.http.RequestListener;
import com.github.tomakehurst.wiremock.verification.NearMiss;
import com.sbg.bdd.resource.ResourceContainer;
import com.sbg.bdd.wiremock.scoped.ScopedWireMock;
import com.sbg.bdd.wiremock.scoped.admin.ScopedAdmin;
import com.sbg.bdd.wiremock.scoped.admin.model.GlobalCorrelationState;
import com.sbg.bdd.wiremock.scoped.admin.model.InitialScopeState;
import com.sbg.bdd.wiremock.scoped.integration.BaseDependencyInjectorAdaptor;
import com.sbg.bdd.wiremock.scoped.server.ScopedWireMockServer;
import org.junit.rules.MethodRule;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import static com.sbg.bdd.wiremock.scoped.common.ExceptionSafe.theCauseOf;
import static com.sbg.bdd.wiremock.scoped.common.Reflection.getStaticValue;
//Not a very useful class. Stops the server after every test
public class ScopedWireMockServerRule extends ScopedWireMock implements TestRule, MethodRule {
    private final boolean failOnUnmatchedStubs;
    protected ScopedWireMockServer server;
    public ScopedWireMockServerRule(WireMockRuleConfiguration options) {
        super(buildAndStartServer(options));
        server= (ScopedWireMockServer) super.admin;
        failOnUnmatchedStubs = options.shouldFailOnUnmatchedStubs();
    }

    private static ScopedAdmin buildAndStartServer(WireMockRuleConfiguration options) {
        ScopedWireMockServer server = new ScopedWireMockServer(options);
        server.start();
        try {
            server.startNewGlobalScope(new GlobalCorrelationState("unit-tests",new URL(server.baseUrl()),null,"unit"));
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return server;
    }

    public ScopedWireMockServerRule(int port) {
        this(WireMockRuleConfiguration.wireMockConfig().port(port));
    }


    public ScopedWireMockServerRule() {
        this(WireMockRuleConfiguration.wireMockConfig().port(Options.DYNAMIC_PORT));
    }
    public void addMockServiceRequestListener(RequestListener listener){
        server.addMockServiceRequestListener(listener);
    }

    @Override
    public Statement apply(final Statement base, Description description) {
        return apply(base, null, null);
    }

    @Override
    public Statement apply(final Statement base, final FrameworkMethod method, final Object target) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                ThreadLocal<WireMock> defaultInstance = getStaticValue(WireMock.class, "defaultInstance");
                defaultInstance.set(ScopedWireMockServerRule.this);
                try {
                    before();
                    base.evaluate();
                    checkForUnmatchedRequests();
                } catch (Throwable e) {
                    throw theCauseOf(e);
                } finally {
                    after();
                    stopServerIfRunningLocally();
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

    public void registerResourceRoot(String root, ResourceContainer resourceRoot) {
        admin.registerResourceRoot(root, resourceRoot);

    }
}
