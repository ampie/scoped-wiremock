package com.sbg.bdd.wiremock.scoped.server;

import com.sbg.bdd.wiremock.scoped.admin.endpointconfig.RemoteEndpointConfigRegistry;
import com.sbg.bdd.wiremock.scoped.admin.model.GlobalCorrelationState;
import com.sbg.bdd.wiremock.scoped.admin.model.JournalMode;
import com.sbg.bdd.wiremock.scoped.common.ParentPath;
import com.sbg.bdd.wiremock.scoped.integration.EndpointConfig;
import org.apache.commons.lang3.StringUtils;

import java.util.SortedSet;

public class GlobalScope extends CorrelatedScope {
    private RemoteEndpointConfigRegistry endPointConfigRegistry;
    private SortedSet<String> allPersonaIds;

    public GlobalScope(GlobalCorrelationState state, SortedSet<String> allPersonaIds) {
        super(null, state.getRunName(), state);
        this.allPersonaIds = allPersonaIds;
        state.setCorrelationPath(toKey(state));
        if (state.getUrlOfServiceUnderTest() != null) {
            String integrationScope = StringUtils.isEmpty(state.getIntegrationScope()) ? EndpointConfig.LOCAL_INTEGRATION_SCOPE : state.getIntegrationScope();
            this.endPointConfigRegistry = new RemoteEndpointConfigRegistry(state.getUrlOfServiceUnderTest().toExternalForm(), integrationScope);
        }
    }

    @Override
    public String getRelativePath() {
        return "";
    }

    public static String toKey(GlobalCorrelationState state) {
        return state.getWireMockPublicUrl().getHost() + "/" + state.getWireMockPublicUrl().getPort() + "/" + state.getRunName() + "/" + state.getSequenceNumber();
    }

    public String getKey() {
        return getCorrelationState().getCorrelationPath();
    }

    @Override
    public GlobalCorrelationState getCorrelationState() {
        return (GlobalCorrelationState) super.getCorrelationState();
    }

    public CorrelatedScope findOrCreateNestedScope(String parentCorrelationPath, String name) {
        CorrelatedScope previousScope = findOrCreateNestedScopeRecursively(parentCorrelationPath);
        return previousScope.findOrCreateNestedScope(name);
    }

    public UserScope findOrCreateUserScope(String parentCorrelationPath, String name) {
        CorrelatedScope previousScope = findOrCreateNestedScopeRecursively(parentCorrelationPath);
        return previousScope.findOrCreateUserScope(name);
    }

    private CorrelatedScope findOrCreateNestedScopeRecursively(String correlationPath) {
        String[] split = correlationPath.split("\\/");
        CorrelatedScope previousScope = this;
        for (int i = 4; i < split.length; i++) {//TODO i dont like the 4
            previousScope = previousScope.findOrCreateNestedScope(split[i]);
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

    public JournalMode getGlobalJournalMode() {
        return getCorrelationState().getGlobalJournaMode();
    }

    public CorrelatedScope findNestedScopeRecursively(String correlationPath) {
        String[] split = correlationPath.split("\\/");
        CorrelatedScope previousScope = this;
        for (int i = 4; i < split.length; i++) {//TODO i dont like the 4
            previousScope = previousScope.getNestedScope(split[i]);
            if (previousScope == null) {
                return null;
            }
        }
        return previousScope;
    }

    public AbstractCorrelatedScope findScopeRecursively(String correlationPath) {
        String parentPath = ParentPath.of(correlationPath);
        CorrelatedScope parent = findNestedScopeRecursively(parentPath);
        if (parent !=null){
            String name = correlationPath.substring(parentPath.length()+1);
            if (name.startsWith(":")) {
                name = name.substring(1);
            }
            return parent.getChild(name);
        }else{
            return null;
        }
    }

    public SortedSet<String> allPersonaIds() {
        return allPersonaIds;
    }
}
