package com.sbg.bdd.wiremock.scoped;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import com.sbg.bdd.resource.ResourceContainer;
import com.sbg.bdd.resource.file.DirectoryResourceRoot;
import com.sbg.bdd.wiremock.scoped.integration.HeaderName;
import org.junit.Test;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public abstract class WhenPlayingBackResponses extends ScopedWireMockTest {
    @Test
    public void should_create_a_mapping_for_each_non_header_file_in_the_resource_directory() throws Exception {
        DirectoryResourceRoot root = getDirectoryResourceRoot();
        getWireMock().serveRecordedMappingsAt(
                (ResourceContainer) root.resolveExisting("some_recording_dir"),
                WireMock.put("/context/service/operation").withHeader(HeaderName.ofTheCorrelationKey(), equalTo("my-correlation-key")).build().getRequest(),
                4
        );
        List<StubMapping> mappings = getWireMock().allStubMappings().getMappings();
        assertThat(mappings.size(), is(2));
    }

}
