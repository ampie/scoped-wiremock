package com.sbg.bdd.wiremock.scoped.server;

import com.sbg.bdd.wiremock.scoped.admin.model.CorrelationState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CorrelatedScope {
    private CorrelatedScope parent;
    private String name;
    private CorrelationState correlationState;
    private Map<String, CorrelatedScope> children = new HashMap<>();

    public CorrelatedScope(CorrelatedScope parent, String name, CorrelationState correlationState) {
        this.parent = parent;
        this.name = name;
        this.correlationState=correlationState;
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

    public static String globalScopeKey(String correlationPath) {
        String[] split = correlationPath.split("\\/");
        if(split.length<4){
            throw new IllegalArgumentException(correlationPath + " is not a valid correlation path");
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 3; i++) {
            sb.append(split[i]);
            sb.append('/');
        }
        sb.append(split[3]);
        return sb.toString();
    }

    public String getCorrelationPath() {
        return correlationState.getCorrelationPath();
    }

    public List<String> getDescendentCorrelationPaths() {
        List<String> result = new ArrayList<>();
        for (CorrelatedScope child : children.values()) {
            result.add(child.getCorrelationPath());
            result.addAll(child.getDescendentCorrelationPaths());
        }
        return result;
    }

    public CorrelatedScope getChild(String name) {
        return children.get(name);
    }

    public void addChild(CorrelatedScope child) {
        children.put(child.getName(),child);
    }

    public List<String> removeChild(CorrelatedScope nestedScope) {
        children.remove(nestedScope.getName());
        List<String> removed = nestedScope.getDescendentCorrelationPaths();
        removed.add(nestedScope.getCorrelationPath());
        return removed;
    }

    public GlobalScope getGlobalScope() {
        return getParent().getGlobalScope();
    }

    public int getLevel() {
        return parent.getLevel() + 1;
    }
}
