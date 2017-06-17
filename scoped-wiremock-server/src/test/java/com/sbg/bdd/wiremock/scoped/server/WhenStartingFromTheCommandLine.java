package com.sbg.bdd.wiremock.scoped.server;

import com.github.tomakehurst.wiremock.standalone.WireMockServerRunner;
import com.sbg.bdd.resource.ResourceRoot;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

public class WhenStartingFromTheCommandLine {
    @Test
    public void should_register_all_resource_roots() {
        ScopedWireMockServerRunner.main("--resourceRoot","journal:/tmp/some/dir");
        ResourceRoot journalRoot = ScopedWireMockServerRunner.getWireMockServer().getResourceRoot("journal").getRoot();
        assertThat(journalRoot.getRootName(), is(
                equalTo("journal")));
    }
}
