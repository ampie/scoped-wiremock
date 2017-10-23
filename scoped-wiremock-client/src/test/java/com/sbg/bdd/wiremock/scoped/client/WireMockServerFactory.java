package com.sbg.bdd.wiremock.scoped.client;

import com.github.tomakehurst.wiremock.core.Options;
import com.google.common.io.Files;
import com.sbg.bdd.resource.ResourceContainer;
import com.sbg.bdd.resource.file.DirectoryResourceRoot;
import com.sbg.bdd.wiremock.scoped.ScopedWireMockTest;
import com.sbg.bdd.wiremock.scoped.admin.ScopedAdmin;
import com.sbg.bdd.wiremock.scoped.server.ScopedWireMockServer;

public class WireMockServerFactory {
    static ScopedWireMockServer server;
    public static ScopedWireMockServer createAndReturnServer() {
        if(server!=null){
            server.shutdown();
        }
        server=new ScopedWireMockServer(Options.DYNAMIC_PORT);
        server.registerResourceRoot("root", ScopedWireMockTest.getDirectoryResourceRoot());
        server.registerResourceRoot("outputRoot", new DirectoryResourceRoot("outputRoot", Files.createTempDir()));
        server.registerResourceRoot(ScopedAdmin.PERSONA_RESOURCE_ROOT, (ResourceContainer) ScopedWireMockTest.getDirectoryResourceRoot().getChild("personas"));
        server.registerResourceRoot(ScopedAdmin.OUTPUT_RESOURCE_ROOT, new DirectoryResourceRoot(ScopedAdmin.OUTPUT_RESOURCE_ROOT, Files.createTempDir()));
        server.registerResourceRoot(ScopedAdmin.JOURNAL_RESOURCE_ROOT, new DirectoryResourceRoot(ScopedAdmin.JOURNAL_RESOURCE_ROOT, Files.createTempDir()));
        server.start();
        return server;
    }

    public static int createAndReturnPort() {
        return createAndReturnServer().port();
    }
}
