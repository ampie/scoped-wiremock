package com.sbg.bdd.wiremock.scoped;

import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.VerificationException;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.sbg.bdd.wiremock.scoped.admin.BadMappingException;
import com.sbg.bdd.wiremock.scoped.integration.HeaderName;
import org.junit.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

public abstract class WhenWorkingOutsideOfAnyScope extends ScopedWireMockTest {

    @Test()
    public void shouldAcceptMappingsWithACorrelationHeaderAndAPriority() {
        MappingBuilder mappingBuilder = get(urlEqualTo("/some/arb/uri")).
                withHeader(HeaderName.ofTheCorrelationKey(), WireMock.equalTo("/some/arb/scope")).
                atPriority(123).
                willReturn(aResponse().proxiedFrom("123413242"));
        getWireMock().register(mappingBuilder);
    }

    @Test()
    public void shouldAcceptMappingsWithADefaultProxyMappingHeaderAndAPriority() {
        MappingBuilder mappingBuilder = get(urlEqualTo("/some/arb/uri")).
                withHeader(HeaderName.toProxyUnmappedEndpoints(), WireMock.equalTo("true")).
                atPriority(123).
                willReturn(aResponse().proxiedFrom("123413242"));
        getWireMock().register(mappingBuilder);
    }

    @Test()
    public void shouldRejectMappingsWithNeitherACorrelationHeaderOrDefaultProxyMappingHeader() {
        try {
            MappingBuilder mappingBuilder = get(urlEqualTo("/some/arb/uri")).
                    atPriority(123).
                    willReturn(aResponse().proxiedFrom("123413242"));
            getWireMock().register(mappingBuilder);
            fail();
        } catch (BadMappingException | VerificationException e) {
            assertThat(e.getMessage(), containsString("Mappings in Correlated WireMock servers must either have a CorrelationPath header or a Proxy header"));
        }
    }

    @Test
    public void shouldRejectMappingsWithoutAPriority() {
        try {
            getWireMock().register(get(urlEqualTo("asdf")).withHeader(HeaderName.ofTheCorrelationKey(), WireMock.equalTo("asdf")).willReturn(aResponse().proxiedFrom("123413242")).build());
            fail();
        } catch (BadMappingException | VerificationException e) {
            assertThat(e.getMessage(), containsString("Mappings in Correlated WireMock servers must have a priority, ideally calculated from the scope it was created"));
        }
    }

    @Test
    public void shouldRejectMappingsWithAScenario() {
        try {
            getWireMock().register(get(urlEqualTo("asdf")).withHeader(HeaderName.ofTheCorrelationKey(), WireMock.equalTo("asdf")).willReturn(aResponse().proxiedFrom("123413242")).atPriority(1).inScenario("some_fake_scenario").build());
            fail();
        } catch (BadMappingException | VerificationException e) {
            assertThat(e.getMessage(), containsString("Mappings in Correlated WireMock servers cannot be associated with a WireMock scenario"));
        }
    }
}
