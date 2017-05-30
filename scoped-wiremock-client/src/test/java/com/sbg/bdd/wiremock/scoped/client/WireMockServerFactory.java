package com.sbg.bdd.wiremock.scoped.client;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.Options;
import com.sbg.bdd.wiremock.scoped.server.ScopedWireMockServer;

public class WireMockServerFactory {
    static WireMockServer server;
    public static int createAndReturnPort() {
        if(server!=null){
            server.shutdown();
        }
        server=new ScopedWireMockServer(Options.DYNAMIC_PORT);
        server.start();
        return server.port();
    }
}
