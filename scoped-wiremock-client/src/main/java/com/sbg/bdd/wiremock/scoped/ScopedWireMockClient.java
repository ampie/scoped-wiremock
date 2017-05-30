package com.sbg.bdd.wiremock.scoped;

import com.sbg.bdd.wiremock.scoped.admin.ScopedAdmin;
import com.sbg.bdd.wiremock.scoped.common.HasBaseUrl;


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

}
