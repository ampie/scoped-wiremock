package com.sbg.bdd.wiremock.scoped;


import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import com.sbg.bdd.wiremock.scoped.ScopedWireMockTest;
import com.sbg.bdd.wiremock.scoped.admin.model.CorrelationState;
import com.sbg.bdd.wiremock.scoped.integration.HeaderName;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class WhenManagingScopedMappings extends ScopedWireMockTest {

    @Test
    public void shouldAddAMappingToASpecificScopeOnly() throws IOException {
        CorrelationState scope1 = getWireMock().joinCorrelatedScope("/scope1");
        CorrelationState scope2 = getWireMock().joinCorrelatedScope("/scope2");
        getWireMock().register(get(urlEqualTo("/test/uri")).withHeader(HeaderName.ofTheCorrelationKey(), matching("/scope1.*")).willReturn(aResponse()).atPriority(1));
        List<StubMapping> mappings = getWireMock().getMappingsInScope("/scope1");
        assertThat(mappings.size(),is(equalTo(1)));
        mappings = getWireMock().getMappingsInScope("/scope2");
        assertThat(mappings.size(),is(equalTo(0)));
    }



    @Test
    public void shouldRemoveAMappingFromASpecificScopeWhenItStops() throws IOException {
        CorrelationState scope1 = getWireMock().joinCorrelatedScope("/scope1");
        CorrelationState scope2 = getWireMock().joinCorrelatedScope("/scope2");
        getWireMock().register(get(urlEqualTo("/test/uri")).withHeader(HeaderName.ofTheCorrelationKey(), matching("/scope1.*")).willReturn(aResponse()).atPriority(1));
        getWireMock().register(get(urlEqualTo("/test/uri")).withHeader(HeaderName.ofTheCorrelationKey(), matching("/scope2.*")).willReturn(aResponse()).atPriority(1));
        List<StubMapping> mappings = getWireMock().getMappingsInScope("/scope1");
        assertThat(mappings.size(),is(equalTo(1)));
        mappings = getWireMock().getMappingsInScope("/scope2");
        assertThat(mappings.size(),is(equalTo(1)));
        getWireMock().stopCorrelatedScope("/scope2");
        mappings = getWireMock().getMappingsInScope("/scope1");
        assertThat(mappings.size(),is(equalTo(1)));
        mappings = getWireMock().getMappingsInScope("/scope2");
        assertThat(mappings.size(),is(equalTo(0)));
    }

}
