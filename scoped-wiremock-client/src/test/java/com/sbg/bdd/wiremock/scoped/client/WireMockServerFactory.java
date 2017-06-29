package com.sbg.bdd.wiremock.scoped.client;

import com.github.tomakehurst.wiremock.core.Options;
import com.sbg.bdd.wiremock.scoped.ScopedWireMockTest;
import com.sbg.bdd.wiremock.scoped.server.ScopedWireMockServer;

public class WireMockServerFactory {
    static ScopedWireMockServer server;
    public static int createAndReturnPort() {
        if(server!=null){
            server.shutdown();
        }
        server=new ScopedWireMockServer(Options.DYNAMIC_PORT);
        server.registerResourceRoot("root", ScopedWireMockTest.getDirectoryResourceRoot());
        server.start();
        return server.port();
    }
}
