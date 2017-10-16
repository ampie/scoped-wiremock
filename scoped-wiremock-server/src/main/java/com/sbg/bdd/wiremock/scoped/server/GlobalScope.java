package com.sbg.bdd.wiremock.scoped.server;

import com.sbg.bdd.wiremock.scoped.admin.endpointconfig.RemoteEndpointConfigRegistry;
import com.sbg.bdd.wiremock.scoped.admin.model.CorrelationState;
import com.sbg.bdd.wiremock.scoped.admin.model.GlobalCorrelationState;
import com.sbg.bdd.wiremock.scoped.integration.EndpointConfig;
import org.apache.commons.lang3.StringUtils;

import java.net.URL;

public class GlobalScope extends CorrelatedScope {
    private RemoteEndpointConfigRegistry endPointConfigRegistry;

    public GlobalScope(GlobalCorrelationState state) {
        super(null, state.getRunName(), state);
        state.setCorrelationPath(toKey(state));
        if (state.getUrlOfServiceUnderTest() != null) {
            String integrationScope = StringUtils.isEmpty(state.getIntegrationScope())?EndpointConfig.LOCAL_INTEGRATION_SCOPE:state.getIntegrationScope();
            this.endPointConfigRegistry = new RemoteEndpointConfigRegistry(state.getUrlOfServiceUnderTest().toExternalForm(), integrationScope);
        }
    }

    public static String toKey(GlobalCorrelationState state) {
        return state.getWireMockPublicUrl().getHost() + "/" + state.getWireMockPublicUrl().getPort() + "/" + state.getRunName()+ "/" + state.getSequenceNumber();
    }

    public String getKey() {
        return getCorrelationState().getCorrelationPath();
    }

    @Override
    public GlobalCorrelationState getCorrelationState() {
        return (GlobalCorrelationState) super.getCorrelationState();
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

    public RemoteEndpointConfigRegistry getEndPointConfigRegistry() {
        return endPointConfigRegistry;
    }
}
