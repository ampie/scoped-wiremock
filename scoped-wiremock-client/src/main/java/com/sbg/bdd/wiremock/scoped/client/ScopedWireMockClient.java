package com.sbg.bdd.wiremock.scoped.client;

import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.core.Admin;
import com.github.tomakehurst.wiremock.matching.RequestPattern;
import com.github.tomakehurst.wiremock.matching.StringValuePattern;
import com.sbg.bdd.resource.ResourceContainer;
import com.sbg.bdd.wiremock.scoped.ScopedWireMock;
import com.sbg.bdd.wiremock.scoped.admin.ScopedAdmin;
import com.sbg.bdd.wiremock.scoped.common.ExchangeRecorder;
import com.sbg.bdd.wiremock.scoped.common.HasBaseUrl;

import java.util.List;


public class ScopedWireMockClient extends ScopedWireMock implements HasBaseUrl {

    public ScopedWireMockClient(ScopedAdmin admin) {
        super(admin);

    }

    public ScopedWireMockClient(int port) {
        this(DEFAULT_HOST, port);
    }

    public ScopedWireMockClient(String host, int port) {
        this(new ScopedHttpAdminClient(host, port));
    }

    public ScopedWireMockClient(String host, int port, String urlPathPrefix) {
        this(new ScopedHttpAdminClient(host, port, urlPathPrefix));
    }

    public ScopedWireMockClient(String scheme, String host, int port) {
        this(new ScopedHttpAdminClient(scheme, host, port));
    }

    public ScopedWireMockClient(String scheme, String host, int port, String urlPathPrefix) {
        this(new ScopedHttpAdminClient(scheme, host, port, urlPathPrefix));
    }

    public ScopedWireMockClient() {
        this(new ScopedHttpAdminClient(DEFAULT_HOST, DEFAULT_PORT));
    }

    public void saveRecordingsForRequestPatternLocally(StringValuePattern scopePath, RequestPattern pattern, ResourceContainer recordingDirectory) {
        addScopePathHeader(scopePath, pattern);
        new ExchangeRecorder(admin, (Admin) admin).saveRecordingsForRequestPattern(pattern, recordingDirectory);
    }

    public List<MappingBuilder> serveLocallyRecordedMappingsAt(ResourceContainer directoryRecordedTo, RequestPattern requestPattern, int priority) {
        return new ExchangeRecorder(admin, (Admin) admin).serveRecordedMappingsAt(directoryRecordedTo, requestPattern, priority);
    }

}
