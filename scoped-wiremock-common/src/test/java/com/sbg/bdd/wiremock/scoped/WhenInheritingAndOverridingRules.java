package com.sbg.bdd.wiremock.scoped;

import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import com.sbg.bdd.wiremock.scoped.admin.model.CorrelationState;
import com.sbg.bdd.wiremock.scoped.integration.HeaderName;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;


public abstract class WhenInheritingAndOverridingRules extends ScopedWireMockTest {

    @Test
    public void shouldInheritMappingsFromContainingScopes() throws IOException {
        CorrelationState rootScope = getWireMock().joinCorrelatedScope("/root_scope");
        CorrelationState nestedScope = getWireMock().startNewCorrelatedScope("/root_scope");
        getWireMock().register(get(urlEqualTo("/test/uri")).withHeader(HeaderName.ofTheCorrelationKey(), matching("/root_scope.*")).willReturn(aResponse()).atPriority(1));
        getWireMock().register(get(urlEqualTo("/test/uri")).withHeader(HeaderName.ofTheCorrelationKey(), matching(nestedScope.getCorrelationPath() +".*")).willReturn(aResponse()).atPriority(1));
        List<StubMapping> mappings = getWireMock().getMappingsInScope("/root_scope");
        assertThat(mappings.size(),is(equalTo(1)));
        mappings = getWireMock().getMappingsInScope(nestedScope.getCorrelationPath());
        assertThat(mappings.size(),is(equalTo(2)));
    }

    @Test
    public void shouldRemoveMappingsRecursivelyFromAContainingScopeWhenItStops() throws IOException {
        CorrelationState rootScope = getWireMock().joinCorrelatedScope("/root_scope");
        CorrelationState nestedScope = getWireMock().startNewCorrelatedScope("/root_scope");
        getWireMock().register(get(urlEqualTo("/test/uri")).withHeader(HeaderName.ofTheCorrelationKey(), matching("/root_scope.*")).willReturn(aResponse()).atPriority(1));
        getWireMock().register(get(urlEqualTo("/test/uri")).withHeader(HeaderName.ofTheCorrelationKey(), matching(nestedScope.getCorrelationPath() +".*")).willReturn(aResponse()).atPriority(1));
        List<StubMapping> mappings = getWireMock().getMappingsInScope("/root_scope");
        assertThat(mappings.size(),is(equalTo(1)));
        mappings = getWireMock().getMappingsInScope(nestedScope.getCorrelationPath());
        assertThat(mappings.size(),is(equalTo(2)));
        getWireMock().stopCorrelatedScope("/root_scope");
        mappings = getWireMock().getMappingsInScope("/root_scope");
        assertThat(mappings.size(),is(equalTo(0)));
        mappings = getWireMock().getMappingsInScope(nestedScope.getCorrelationPath());
        assertThat(mappings.size(),is(equalTo(0)));
    }
}
