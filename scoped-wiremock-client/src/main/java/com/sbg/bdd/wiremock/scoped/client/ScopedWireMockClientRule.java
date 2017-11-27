package com.sbg.bdd.wiremock.scoped.client;


import com.github.tomakehurst.wiremock.client.VerificationException;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import com.github.tomakehurst.wiremock.verification.NearMiss;
import com.sbg.bdd.wiremock.scoped.admin.ScopedAdmin;
import com.sbg.bdd.wiremock.scoped.admin.model.CorrelationState;
import com.sbg.bdd.wiremock.scoped.admin.model.GlobalCorrelationState;
import com.sbg.bdd.wiremock.scoped.admin.model.InitialScopeState;
import com.sbg.bdd.wiremock.scoped.client.junit.ScopedAdminHolder;
import com.sbg.bdd.wiremock.scoped.common.Reflection;
import com.sbg.bdd.wiremock.scoped.integration.BaseDependencyInjectorAdaptor;
import org.junit.rules.MethodRule;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ScopedWireMockClientRule extends ScopedWireMockClient implements TestRule, MethodRule {
    private static Logger LOGGER=Logger.getLogger(ScopedWireMockClientRule.class.getName());
    private boolean failOnUnmatchedStubs = false;

    public ScopedWireMockClientRule(ScopedAdmin admin) {
        super(admin);
        ScopedAdminHolder.setScopedAdmin(admin);
    }
    @Override
    public Statement apply(final Statement base, Description description) {
        return apply(base, description.toString());
    }

    @Override
    public Statement apply(final Statement base, final FrameworkMethod method, Object target) {
        final String name = method.getDeclaringClass().getSimpleName() + "_" + method.getMethod();
        return apply(base, name);
    }

    private Statement apply(final Statement base, final String name)  {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                ThreadLocal<WireMock> defaultInstance = Reflection.getStaticValue(WireMock.class, "defaultInstance");
                defaultInstance.set(ScopedWireMockClientRule.this);
                CorrelationState nestedScope=null;
                try {
                    String parentPath = ScopedAdminHolder.getGlobalCorrelationState().getCorrelationPath();
                    nestedScope = admin.startNestedScope(new InitialScopeState(parentPath, name, Collections.<String, Object>emptyMap()));
                    BaseDependencyInjectorAdaptor.CURRENT_CORRELATION_STATE.set(nestedScope.getCorrelationPath(),1,false);
                    before();
                    base.evaluate();
                    checkForUnmatchedRequests();
                } finally {
                    after();
                    try {
                        admin.stopNestedScope(nestedScope);
                    } catch (Exception e) {
                        LOGGER.log(Level.WARNING,"Could not stop nested scope",e);
                    }
                }
            }

        };
    }

    private void checkForUnmatchedRequests() {
        if (failOnUnmatchedStubs) {
            List<NearMiss> nearMisses = findNearMissesForAllUnmatchedRequests();
            if (!nearMisses.isEmpty()) {

                List<LoggedRequest> logRequests=new ArrayList<>();
                for (NearMiss nearMiss : nearMisses) {
                    logRequests.add(nearMiss.getRequest());
                }
                throw VerificationException.forUnmatchedRequests(logRequests);
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
