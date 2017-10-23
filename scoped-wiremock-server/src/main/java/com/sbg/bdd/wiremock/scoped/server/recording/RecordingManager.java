package com.sbg.bdd.wiremock.scoped.server.recording;


import com.google.common.base.Optional;
import com.sbg.bdd.resource.ReadableResource;
import com.sbg.bdd.resource.Resource;
import com.sbg.bdd.resource.ResourceContainer;
import com.sbg.bdd.resource.ResourceFilter;
import com.sbg.bdd.wiremock.scoped.admin.ScopedAdmin;
import com.sbg.bdd.wiremock.scoped.admin.model.ExtendedStubMapping;
import com.sbg.bdd.wiremock.scoped.admin.model.JournalMode;
import com.sbg.bdd.wiremock.scoped.server.AbstractCorrelatedScope;
import com.sbg.bdd.wiremock.scoped.server.CorrelatedScope;
import com.sbg.bdd.wiremock.scoped.server.UserScope;

import java.util.*;

public class RecordingManager {

    private ScopedAdmin scopedAdmin;

    public RecordingManager(ScopedAdmin scopedAdmin) {
        this.scopedAdmin = scopedAdmin;
    }


    public void processRecordingSpec(ExtendedStubMapping builder, AbstractCorrelatedScope scope) {
        if (!shouldIgnoreMapping(builder, scope)) {
            if (scope instanceof CorrelatedScope) {
                for (String personaDir : allPersonaIds()) {
                    processRecordingSpecs(builder, ((CorrelatedScope) scope).findOrCreateUserScope(personaDir));
                }
            } else {
                processRecordingSpecs(builder, (UserScope) scope);
            }
        }
    }

    protected void processRecordingSpecs(ExtendedStubMapping builder, UserScope userInScope) {
        if (builder.getRecordingSpecification().getJournalModeOverride() == JournalMode.RECORD) {
            userInScope.addRecordingMapping(new RecordingMappingForUser(scopedAdmin, userInScope.getName(), builder));
        } else if (builder.getRecordingSpecification().getJournalModeOverride() == JournalMode.PLAYBACK) {
            RecordingMappingForUser recordingMappingForUser = new RecordingMappingForUser(scopedAdmin, userInScope.getName(), builder);
            userInScope.addRecordingMapping(recordingMappingForUser);
            recordingMappingForUser.loadRecordings(userInScope.getParent());
        } else if (builder.getRecordingSpecification().enforceJournalModeInScope()) {
            RecordingMappingForUser recordingMappingForUser = new RecordingMappingForUser(scopedAdmin, userInScope.getName(), builder);
            userInScope.addRecordingMapping(recordingMappingForUser);
            if (getJournalModeInScope(userInScope) == JournalMode.PLAYBACK) {
                recordingMappingForUser.loadRecordings(userInScope.getParent());
            }
        }
    }

    private Set<String> allPersonaIds() {
        ResourceContainer resourceRoot = scopedAdmin.getResourceRoot(ScopedAdmin.PERSONA_RESOURCE_ROOT);
        if(resourceRoot==null){
            return Collections.emptySet();
        }
        List<Resource> list = Arrays.asList(resourceRoot.list(new ResourceFilter() {

            @Override
            public boolean accept(ResourceContainer dir, String name) {
                Resource file = dir.resolveExisting(name);
                if (file.getName().equals("everybody")) {
                    return false;
                } else if (file instanceof ResourceContainer) {
                    return file.getName().equals(ScopedAdmin.GUEST) || hasPersonaFile((ResourceContainer) file);
                } else {
                    return false;
                }
            }

            private boolean hasPersonaFile(ResourceContainer file) {
                for (Resource resource : file.list()) {
                    if(resource instanceof ReadableResource && resource.getName().startsWith("persona.")){
                        return true;
                    }
                }
                return false;
            }

        }));
        TreeSet<String> result = new TreeSet<>();
        for (Resource o : list) {
            result.add(o.getName());
        }
        return result;
    }

    private JournalMode getJournalModeInScope(AbstractCorrelatedScope scope) {
        return Optional.fromNullable(scope.getGlobalScope().getGlobalJournalMode()).or(JournalMode.NONE);
    }

    public boolean shouldIgnoreMapping(ExtendedStubMapping stubMapping, AbstractCorrelatedScope scope) {
        //In playback mode we ignore all builders, except those that enforce the journalModeInScope, which of course is playback
        return getJournalModeInScope(scope) == JournalMode.PLAYBACK && stubMapping.getRecordingSpecification() != null && !stubMapping.getRecordingSpecification().enforceJournalModeInScope();
    }

    public void saveRecordings(CorrelatedScope scene) {
        for (RecordingMappingForUser m : getActiveRecordingOrPlaybackMappings(scene, JournalMode.RECORD)) {
            if (scene.getUserScopes().containsKey(m.getUserInScopeId())) {
                m.saveRecordings(scene);
            }
        }
    }

    public void loadRecordings(CorrelatedScope scene) {
        for (RecordingMappingForUser m : getActiveRecordingOrPlaybackMappings(scene, JournalMode.PLAYBACK)) {
            m.loadRecordings(scene);
        }
    }

    public List<RecordingMappingForUser> getActiveRecordingOrPlaybackMappings(CorrelatedScope scene, JournalMode journalMode) {
        List<RecordingMappingForUser> activeRecordings = new ArrayList<>();
        CorrelatedScope parentScene = scene.getParent();
        if (parentScene != null) {
            activeRecordings.addAll(getActiveRecordingOrPlaybackMappings(parentScene, journalMode));
        }
        for (String personaId : allPersonaIds()) {
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
}
