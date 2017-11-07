package com.sbg.bdd.wiremock.scoped.server;

import com.google.common.base.Optional;
import com.sbg.bdd.wiremock.scoped.admin.model.CorrelationState;
import com.sbg.bdd.wiremock.scoped.admin.model.JournalMode;
import com.sbg.bdd.wiremock.scoped.server.recording.RecordingMappingForUser;

import java.util.*;

public class CorrelatedScope extends AbstractCorrelatedScope {
    private Map<String, CorrelatedScope> nestedScopes = new HashMap<>();
    private Map<String, UserScope> userScopes = new HashMap<>();


    public CorrelatedScope(CorrelatedScope parent, String name, CorrelationState correlationState) {
        super(parent, name, correlationState);
    }

    public Map<String, UserScope> getUserScopes() {
        return userScopes;
    }

    public static String globalScopeKey(String correlationPath) {
        String[] split = correlationPath.split("\\/");
        if (split.length < 4) {
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

    public List<String> getDescendentCorrelationPaths() {
        List<String> result = new ArrayList<>();
        for (CorrelatedScope nested : nestedScopes.values()) {
            result.add(nested.getCorrelationPath());
            result.addAll(nested.getDescendentCorrelationPaths());
        }
        for (UserScope child : userScopes.values()) {
            result.add(child.getCorrelationPath());
        }
        return result;
    }

    public UserScope findOrCreateUserScope(String name) {
        UserScope userScope = userScopes.get(name);
        if (userScope == null) {
            userScopes.put(name, userScope = new UserScope(this, name, new CorrelationState(getCorrelationPath() + "/:" + name)));
        }
        return userScope;
    }

    public CorrelatedScope findOrCreateNestedScope(String name) {
        CorrelatedScope nestedScope = nestedScopes.get(name);
        if (nestedScope == null) {
            nestedScopes.put(name, nestedScope = new CorrelatedScope(this, name, new CorrelationState(getCorrelationPath() + "/" + name)));
        }
        return nestedScope;
    }

    public AbstractCorrelatedScope getChild(String name) {
        AbstractCorrelatedScope scope = nestedScopes.get(name);
        if (scope == null) {
            scope = userScopes.get(name);
        }
        return scope;
    }


    public List<String> removeNestedScope(CorrelatedScope nestedScope) {
        nestedScopes.remove(nestedScope.getName());
        List<String> removed = nestedScope.getDescendentCorrelationPaths();
        removed.add(nestedScope.getCorrelationPath());
        return removed;
    }


    public String getRelativePath() {
        if (getParent() instanceof GlobalScope) {
            return getName();
        } else {
            return getParent().getRelativePath() + "/" + getName();
        }
    }

    public UserScope getUserScope(String name) {
        return getUserScopes().get(name);
    }

    public CorrelatedScope getNestedScope(String name) {
        return nestedScopes.get(name);
    }

    public List<RecordingMappingForUser> getActiveRecordingOrPlaybackMappings(JournalMode playback) {
        return getActiveRecordingOrPlaybackMappings(this, playback);
    }

    //TODO move this to AbstractCorrelatedScope
    private List<RecordingMappingForUser> getActiveRecordingOrPlaybackMappings(CorrelatedScope scene, JournalMode journalMode) {
        List<RecordingMappingForUser> activeRecordings = new ArrayList<>();
        CorrelatedScope parentScene = scene.getParent();
        if (parentScene != null) {
            activeRecordings.addAll(getActiveRecordingOrPlaybackMappings(parentScene, journalMode));
        }
        for (String personaId : getGlobalScope().allPersonaIds()) {
            UserScope userScope = scene.getUserScope(personaId);
            if (userScope != null) {
                activeRecordings.addAll(getRecordingOrPlaybackMappings(userScope, journalMode));
            }
        }
        return activeRecordings;
    }

    private List<RecordingMappingForUser> getRecordingOrPlaybackMappings(UserScope userInScope, JournalMode journalMode) {
        List<RecordingMappingForUser> result = new ArrayList<>();
        List<RecordingMappingForUser> requestsToRecordOrPlayback = userInScope.getRecordingMappingsForUser();
        if (requestsToRecordOrPlayback != null) {
            for (RecordingMappingForUser r : requestsToRecordOrPlayback) {
                if (r.getJournalModeOverride() == journalMode || (r.enforceJournalModeInScope() && getJournalModeInScope(userInScope) == journalMode)) {
                    result.add(r);
                }
            }
        }
        return result;
    }

    private JournalMode getJournalModeInScope(UserScope userInScope) {
        return Optional.fromNullable(userInScope.getGlobalScope().getGlobalJournalMode()).or(JournalMode.NONE);
    }

    public Map<String, Object> aggregateTemplateVariables() {
        Map<String, Object> result = new HashMap<>();
        addTemplateVariablesFromAncestors(this, null, result);
        return result;
    }
}
