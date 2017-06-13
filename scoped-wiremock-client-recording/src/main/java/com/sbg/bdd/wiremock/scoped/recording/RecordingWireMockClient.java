package com.sbg.bdd.wiremock.scoped.recording;


import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.core.Admin;
import com.github.tomakehurst.wiremock.matching.RequestPattern;
import com.github.tomakehurst.wiremock.matching.StringValuePattern;
import com.sbg.bdd.resource.ResourceContainer;
import com.sbg.bdd.wiremock.scoped.ScopedWireMockClient;
import com.sbg.bdd.wiremock.scoped.common.ExchangeRecorder;
import com.sbg.bdd.wiremock.scoped.admin.ScopedAdmin;

import java.net.URL;
import java.util.List;


public class RecordingWireMockClient extends ScopedWireMockClient {

    public RecordingWireMockClient(ScopedAdmin admin) {
        super(admin);
    }

    public RecordingWireMockClient(URL wireMockBaseUrl) {
        super(wireMockBaseUrl.getHost(), wireMockBaseUrl.getPort(), wireMockBaseUrl.getPath());
    }


    public void saveRecordingsForRequestPatternLocally(StringValuePattern scopePath, RequestPattern pattern, ResourceContainer recordingDirectory) {
        addScopePathHeader(scopePath, pattern);
        new ExchangeRecorder(admin, (Admin) admin).saveRecordingsForRequestPattern(pattern, recordingDirectory);
    }

    public List<MappingBuilder> serveLocallyRecordedMappingsAt(ResourceContainer directoryRecordedTo, RequestPattern requestPattern, int priority) {
        return new ExchangeRecorder(admin, (Admin) admin).serveRecordedMappingsAt(directoryRecordedTo, requestPattern, priority);
    }
}
