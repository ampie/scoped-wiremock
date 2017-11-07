package com.sbg.bdd.wiremock.scoped.server.recording;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.matching.MultiValuePattern;
import com.github.tomakehurst.wiremock.matching.RequestPattern;
import com.sbg.bdd.resource.ResourceContainer;
import com.sbg.bdd.resource.file.DirectoryResourceRoot;
import com.sbg.bdd.wiremock.scoped.admin.ScopedAdmin;
import com.sbg.bdd.wiremock.scoped.admin.model.*;

import com.sbg.bdd.wiremock.scoped.integration.HeaderName;
import com.sbg.bdd.wiremock.scoped.server.CorrelatedScope;
import com.sbg.bdd.wiremock.scoped.server.ExtendedStubMappingTranslator;
import com.sbg.bdd.wiremock.scoped.server.UserScope;

import java.io.File;


//TODO refactor out of existence Move logic to AbstractCorrelatedScope and ExchangeRecorder
public class RecordingMappingForUser {
    private UserScope userScope;
    private ExtendedStubMapping stubMapping;


    public RecordingMappingForUser(UserScope userScope, ExtendedStubMapping stubMapping) {
        this.userScope = userScope;
        this.stubMapping = stubMapping;
    }

    public String getUserInScopeId() {
        return userScope.getName();
    }
    public RecordingSpecification getRecordingSpecification() {
        return this.stubMapping.getRecordingSpecification();
    }
    public boolean enforceJournalModeInScope() {
        return getRecordingSpecification().enforceJournalModeInScope();
    }

    public JournalMode getJournalModeOverride() {
        return getRecordingSpecification().getJournalModeOverride();
    }


    public ScopeLocalPriority priority() {
        return getRecordingSpecification().enforceJournalModeInScope() ? ScopeLocalPriority.JOURNAL : ScopeLocalPriority.RECORDINGS;
    }

    public MultiValuePattern deriveCorrelationPath(CorrelatedScope scope) {
        if (stubMapping.getRecordingSpecification().enforceJournalModeInScope()) {
            //Exact match because we want consistent journal behaviour reflecting the recording's exact location
            return MultiValuePattern.of(WireMock.equalTo(scope.getCorrelationPath() + "/:" + userScope.getName()));
        } else {
            //Pattern match because we may want to move the directory around
            return MultiValuePattern.of(WireMock.matching(scope.getCorrelationPath() + "/.*:" + userScope.getName()));
        }
    }


    public RequestPattern getRequest() {
        return stubMapping.getRequest();
    }

    public UserScope getUserScope() {
        return userScope;
    }
}
