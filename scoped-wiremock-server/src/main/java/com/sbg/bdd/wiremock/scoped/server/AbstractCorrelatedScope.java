package com.sbg.bdd.wiremock.scoped.server;

import com.sbg.bdd.wiremock.scoped.admin.model.CorrelationState;

import java.util.HashMap;
import java.util.Map;


public abstract class AbstractCorrelatedScope {
    protected CorrelatedScope parent;
    protected String name;
    protected CorrelationState correlationState;
    private Map<String, Object> templateVariables=new HashMap<>();

    public AbstractCorrelatedScope(CorrelatedScope parent, String name, CorrelationState correlationState) {
        this.parent = parent;
        this.name = name;
        this.correlationState=correlationState;
    }

    public int getLevel() {
        return parent.getLevel() + 1;
    }
    public CorrelatedScope getParent() {
        return parent;
    }

    public String getName() {
        return name;
    }

    public CorrelationState getCorrelationState() {
        return correlationState;
    }

    public String getCorrelationPath() {
        return correlationState.getCorrelationPath();
    }

    public GlobalScope getGlobalScope() {
        return getParent().getGlobalScope();
    }

    public Map<String, Object> getTemplateVariables() {
        return templateVariables;
    }

    protected void addTemplateVariablesFromAncestors(CorrelatedScope scope, String userScopeId, Map<String, Object> templateVariables) {
        if (scope.getParent() != null) {
            addTemplateVariablesFromAncestors(scope.getParent(), userScopeId, templateVariables);
        }
        templateVariables.putAll(scope.getTemplateVariables());
        if (userScopeId != null && scope.getUserScope(userScopeId) != null) {
            //userscope overrides everybodyscope
            templateVariables.putAll(scope.getUserScope(userScopeId).getTemplateVariables());
        }

    }

    public abstract Map<String, Object>  aggregateTemplateVariables() ;
}
