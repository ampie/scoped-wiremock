package com.sbg.bdd.wiremock.scoped.server;

import com.google.common.base.Optional;
import com.sbg.bdd.wiremock.scoped.admin.model.CorrelationState;
import com.sbg.bdd.wiremock.scoped.admin.model.JournalMode;
import com.sbg.bdd.wiremock.scoped.server.recording.RecordingMappingForUser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserScope extends AbstractCorrelatedScope {
    List<RecordingMappingForUser> recordingMappingsForUser = new ArrayList<>();

    public UserScope(CorrelatedScope parent, String name, CorrelationState correlationState) {
        super(parent, name, correlationState);
    }

    public List<RecordingMappingForUser> getRecordingMappingsForUser() {
        return recordingMappingsForUser;
    }

    public void addRecordingMapping(RecordingMappingForUser r) {
        recordingMappingsForUser.add(r);
    }

    public JournalMode getJournalModeInScope() {
        return Optional.fromNullable(getGlobalScope().getGlobalJournalMode()).or(JournalMode.NONE);
    }

    public Map<String, Object> aggregateTemplateVariables() {
        Map<String, Object> result = new HashMap<>();
        addTemplateVariablesFromAncestors(getParent(), getName(), result);
        return result;
    }
}
