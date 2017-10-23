package com.sbg.bdd.wiremock.scoped.server;

import com.sbg.bdd.wiremock.scoped.admin.model.CorrelationState;
import com.sbg.bdd.wiremock.scoped.server.recording.RecordingMappingForUser;

import java.util.ArrayList;
import java.util.List;

public class UserScope extends AbstractCorrelatedScope {
    List<RecordingMappingForUser> recordingMappingsForUser = new ArrayList<>();
    public UserScope(CorrelatedScope parent, String name, CorrelationState correlationState) {
        super(parent, name, correlationState);
    }

    public List<RecordingMappingForUser> getRecordingMappingsForUser() {
        return recordingMappingsForUser;
    }
    public void addRecordingMapping(RecordingMappingForUser r){
        recordingMappingsForUser.add(r);
    }
}
