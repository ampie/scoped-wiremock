package com.github.ampie.wiremock.junit;


import com.github.tomakehurst.wiremock.client.VerificationException;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.verification.NearMiss;
import com.github.ampie.wiremock.ScopedWireMockServer;
import com.github.ampie.wiremock.client.ScopedWireMock;
import org.junit.rules.MethodRule;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

import java.util.List;

import static com.github.ampie.wiremock.common.Reflection.getStaticValue;
import static com.github.ampie.wiremock.common.ExceptionSafe.theCauseOf;

public class ScopedWireMockServerRule extends ScopedWireMockServer implements TestRule, MethodRule {
    private final boolean failOnUnmatchedStubs;
    private ScopedWireMock client;

    public ScopedWireMockServerRule(WireMockRuleConfiguration options) {
        super(options);
        failOnUnmatchedStubs=options.shouldFailOnUnmatchedStubs();
        start();
    }

    public ScopedWireMockServerRule(int port) {
        this(WireMockRuleConfiguration.wireMockConfig().port(port));
    }


    public ScopedWireMockServerRule() {
        this(WireMockRuleConfiguration.wireMockConfig());
    }

    public ScopedWireMock getClient() {
        if (client == null) {
            client = new ScopedWireMock(this);
        }
        return client;
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
                defaultInstance.set(getClient());
                try {
                    before();
                    setCurrentCorrelationState(target, base, method);
                    base.evaluate();
                    checkForUnmatchedRequests();
                }catch(Throwable e){
                    throw theCauseOf(e);
                } finally {
                    after();
                    stop();
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

}
