package com.sbg.bdd.wiremock.scoped.server.junit;


import com.github.tomakehurst.wiremock.client.VerificationException;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.Options;
import com.github.tomakehurst.wiremock.verification.NearMiss;
import com.sbg.bdd.resource.ResourceRoot;
import com.sbg.bdd.resource.file.DirectoryResourceRoot;
import com.sbg.bdd.wiremock.scoped.ScopedWireMock;
import com.sbg.bdd.wiremock.scoped.server.ScopedWireMockServer;
import com.sbg.bdd.wiremock.scoped.admin.ScopedAdmin;
import org.junit.rules.MethodRule;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

import java.util.List;

import static com.sbg.bdd.wiremock.scoped.common.ExceptionSafe.theCauseOf;
import static com.sbg.bdd.wiremock.scoped.common.Reflection.getStaticValue;

public class ScopedWireMockServerRule extends ScopedWireMock implements TestRule, MethodRule {
    private final boolean failOnUnmatchedStubs;
    private ScopedWireMock client;

    public ScopedWireMockServerRule(WireMockRuleConfiguration options) {
        super(buildAndStartServer(options));
        failOnUnmatchedStubs = options.shouldFailOnUnmatchedStubs();
    }

    private static ScopedAdmin buildAndStartServer(WireMockRuleConfiguration options) {
        ScopedWireMockServer server = new ScopedWireMockServer(options);
        server.start();
        return server;
    }

    public ScopedWireMockServerRule(int port) {
        this(WireMockRuleConfiguration.wireMockConfig().port(port));
    }


    public ScopedWireMockServerRule() {
        this(WireMockRuleConfiguration.wireMockConfig().port(Options.DYNAMIC_PORT));
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
                    setCurrentCorrelationState(target, base, method);
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

    private void setCurrentCorrelationState(Object target, Statement base, FrameworkMethod method) {
//        Object actualTarget = target;
//        String scopePath = "localhost/" + port() + "/";
//        if(actualTarget == null){
//            try{
//                actualTarget=getValue(base,"target");
//            }catch(IllegalArgumentException e){}
//        }
//        scopePath += actualTarget.getClass().getSimpleName();
//        if(method!=null){
//            scopePath =scopePath +"/" + method.getName();
//        }
//        WireMockCorrelationState state = new WireMockCorrelationState();
//        state.set(scopePath,false);//false because we don't want to have to register all the freakin mappings for unit tests.
//        WireMockCorrelationState.setCurrentWireMockCorrelationState(state);
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

    public void registerResourceRoot(String root, ResourceRoot resourceRoot) {
        admin.registerResourceRoot(root, resourceRoot);

    }
}
