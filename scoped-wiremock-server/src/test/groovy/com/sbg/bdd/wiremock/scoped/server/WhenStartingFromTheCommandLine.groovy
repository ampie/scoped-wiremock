package com.sbg.bdd.wiremock.scoped.server

import spock.lang.Specification

class WhenStartingFromTheCommandLine extends Specification {
    def 'register all resource roots'() {
        when: 'I start the ScopedWireMock server with a resource root named "journal"'
        ScopedWireMockServerRunner.main("--resourceRoot", "journal:/tmp/some/dir");
        then: 'the resource root named"journal" should reflect the correct directory'
        def journalRoot = ScopedWireMockServerRunner.getWireMockServer().getResourceRoot("journal").getRoot();
        journalRoot.getRootName() == 'journal'
        journalRoot.file.absolutePath == '/tmp/some/dir'
    }
    def cleanup(){
        try{
            ScopedWireMockServerRunner.wireMockServer.shutdownServer()
        }catch(Exception e){

        }
    }
}
