package com.sbg.bdd.wiremock.scoped.server.recording;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.matching.MultiValuePattern;
import com.sbg.bdd.resource.ResourceContainer;
import com.sbg.bdd.resource.file.DirectoryResourceRoot;
import com.sbg.bdd.wiremock.scoped.admin.ScopedAdmin;
import com.sbg.bdd.wiremock.scoped.admin.model.*;

import com.sbg.bdd.wiremock.scoped.integration.HeaderName;
import com.sbg.bdd.wiremock.scoped.server.CorrelatedScope;
import com.sbg.bdd.wiremock.scoped.server.ExtendedStubMappingCreator;

import java.io.File;


//TODO move to server
public class RecordingMappingForUser {
    private static final String EVERYBODY = "everybody";
    private ExtendedStubMapping stubMapping;
    private String userInScopeId;
    private ScopedAdmin scopedAdmin;

    public RecordingMappingForUser(ScopedAdmin scopedAdmin, String userScopeName, ExtendedStubMapping stubMapping) {
        this.scopedAdmin = scopedAdmin;
        if (userScopeName.equals(EVERYBODY)) {
            throw new IllegalArgumentException();
        }
        this.userInScopeId = userScopeName;
        this.stubMapping = stubMapping;
    }

    public String getUserInScopeId() {
        return userInScopeId;
    }

    public void saveRecordings(CorrelatedScope scope) {
        ExtendedRequestPattern requestPattern = new ExtendedRequestPattern(scope.getCorrelationPath() + "/:" + userInScopeId, stubMapping.getRequest());
        ScopedAdmin wireMock = getWireMock();
        requestPattern.getHeaders().put(HeaderName.ofTheCorrelationKey(), deriveCorrelationPath(scope));
        wireMock.saveRecordingsForRequestPattern(requestPattern, calculateRecordingDirectory(scope));
    }


    public ScopedAdmin getWireMock() {
        return this.scopedAdmin;
    }

    public void loadRecordings(CorrelatedScope scope) {
        ResourceContainer recordingDirectory = calculateRecordingDirectory(scope, userInScopeId);
        if (recordingDirectory != null) {
            //may not exist
            ScopedAdmin wireMock = getWireMock();
            ExtendedRequestPattern requestPattern = new ExtendedRequestPattern(scope.getCorrelationPath() + "/:" + userInScopeId, stubMapping.getRequest());
            requestPattern.getHeaders().put(HeaderName.ofTheCorrelationKey(), deriveCorrelationPath(scope));
            wireMock.serveRecordedMappingsAt(recordingDirectory, requestPattern, ExtendedStubMappingCreator.calculatePriority(priority(), scope.findOrCreateUserScope(userInScopeId)));
        }
    }

    private MultiValuePattern deriveCorrelationPath(CorrelatedScope scope) {
        if (stubMapping.getRecordingSpecification().enforceJournalModeInScope()) {
            //Exact match because we want consistent journal behaviour reflecting the recording's exact location
            return MultiValuePattern.of(WireMock.equalTo(scope.getCorrelationPath() + "/:" + userInScopeId));
        } else {
            //Pattern match because we may want to move the directory around
            return MultiValuePattern.of(WireMock.matching(scope.getCorrelationPath() + "/.*:" + userInScopeId));
        }
    }

    public boolean enforceJournalModeInScope() {
        return getRecordingSpecification().enforceJournalModeInScope();
    }

    public JournalMode getJournalModeOverride() {
        return getRecordingSpecification().getJournalModeOverride();
    }


    private ScopeLocalPriority priority() {
        return getRecordingSpecification().enforceJournalModeInScope() ? ScopeLocalPriority.JOURNAL : ScopeLocalPriority.RECORDINGS;
    }

    public ResourceContainer calculateRecordingDirectory(CorrelatedScope scope) {
        return calculateRecordingDirectory(scope, this.userInScopeId);
    }

    private ResourceContainer calculateRecordingDirectory(CorrelatedScope scope, String userScopeIdToUse) {
        if (getRecordingSpecification().enforceJournalModeInScope()) {
            //scoped based journalling is assumed to be an automated process where potentially huge amounts of exchanges are recorded and never checked it.
            //if we wanted to investigate what went wrong, we are more interested in the run scope than the persona
            //hence runscope1/runscope1.1/scenarioscope1.1.1/userInScopeId
            if (getRecordingSpecification().recordToCurrentResourceDir()) {
                //Record to journalRoot in scope
                return toFile(getResourceRoot(), scope.getRelativePath(), userScopeIdToUse);
            } else if (!getResourceRoot().fallsWithin(getRecordingSpecification().getRecordingDirectory())) {
                return toFile(getAbsoluteRecordingDir(), scope.getRelativePath(), userScopeIdToUse);
            } else {
                return toFile(getResourceRoot(), getRecordingSpecification().getRecordingDirectory(), scope.getRelativePath(), userScopeIdToUse);
            }
        } else {
            //explicit recording mapping is assumed to be a more manual process during development, fewer exchanges will
            //be recorded, possibly manually modified or converted to
            //templates, and then eventually be checked in
            //process where we are more interested in the persona associated with the exchanges
            //hence userScope_id/runscope1 / runscope1 .1 / scenarioscope1 .1 .1
            if (getRecordingSpecification().recordToCurrentResourceDir()) {
                return toFile(getResourceRoot(), userScopeIdToUse, scope.getRelativePath());
            } else if (!getResourceRoot().fallsWithin(getRecordingSpecification().getRecordingDirectory())) {
                //unlikely to be used this way
                return toFile(getAbsoluteRecordingDir(), userScopeIdToUse, scope.getRelativePath());
            } else {
                //somewhere in the checked in persona dir, relative to the current resource dir
                return toFile(getResourceRoot(), userScopeIdToUse, scope.getRelativePath(), getRecordingSpecification().getRecordingDirectory());
            }
        }
    }

    private DirectoryResourceRoot getAbsoluteRecordingDir() {
        return new DirectoryResourceRoot("absoluteDir", new File(getRecordingSpecification().getRecordingDirectory()));
    }


    private ResourceContainer getResourceRoot() {
        if (getRecordingSpecification().enforceJournalModeInScope()) {
            return getWireMock().getResourceRoot(ScopedAdmin.JOURNAL_RESOURCE_ROOT);
        } else if (getRecordingSpecification().getJournalModeOverride() == JournalMode.RECORD) {
            return getWireMock().getResourceRoot(ScopedAdmin.OUTPUT_RESOURCE_ROOT);
        } else {
            return getWireMock().getResourceRoot(ScopedAdmin.PERSONA_RESOURCE_ROOT);
        }
    }

    private RecordingSpecification getRecordingSpecification() {
        return this.stubMapping.getRecordingSpecification();
    }

    private ResourceContainer toFile(ResourceContainer root, String... trailingSegments) {
        return root.resolvePotentialContainer(trailingSegments);
    }

}
