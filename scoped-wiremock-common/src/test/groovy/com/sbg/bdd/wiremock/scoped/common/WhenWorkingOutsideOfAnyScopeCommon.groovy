package com.sbg.bdd.wiremock.scoped.common

import com.github.tomakehurst.wiremock.client.MappingBuilder
import com.github.tomakehurst.wiremock.client.VerificationException
import com.github.tomakehurst.wiremock.client.WireMock
import com.sbg.bdd.wiremock.scoped.admin.BadMappingException
import com.sbg.bdd.wiremock.scoped.integration.HeaderName

import static com.github.tomakehurst.wiremock.client.WireMock.*
import static org.junit.Assert.fail

abstract class WhenWorkingOutsideOfAnyScopeCommon extends ScopedWireMockCommonTest {

    def 'register a mapping with a correlationKey header and a priority'() {
        when: 'I register a mapping with a correlationKey header and a priority'
        MappingBuilder mappingBuilder = get(urlEqualTo("/some/arb/uri")).
                withHeader(HeaderName.ofTheCorrelationKey(), WireMock.equalTo("/some/arb/scope")).
                atPriority(123).
                willReturn(aResponse().proxiedFrom("123413242"))
        def mapping = wireMock.register(mappingBuilder)
        then: 'it should succeed'
        mapping != null
    }

    def 'register a mapping with default proxying enabled and a priority'() {
        when: 'I register a mapping with priority and I have enabled default proxy mapping'
        MappingBuilder mappingBuilder = get(urlEqualTo("/some/arb/uri")).
                withHeader(HeaderName.toProxyUnmappedEndpoints(), WireMock.equalTo("true")).
                atPriority(123).
                willReturn(aResponse().proxiedFrom("123413242"))
        def mapping = wireMock.register(mappingBuilder)
        then: 'it should succeed'
    }

    def 'shouldRejectMappingsWithNeitherACorrelationHeaderOrDefaultProxyMappingHeader'() {
        when: 'I register a mapping with neither a correlationKey header or defaultProxying'
        def exception = null
        try {
            MappingBuilder mappingBuilder = get(urlEqualTo("/some/arb/uri")).
                    atPriority(123).
                    willReturn(aResponse().proxiedFrom("123413242"));
            wireMock.register(mappingBuilder)
            fail()
        } catch (BadMappingException | VerificationException e) {
            exception = e
        }
        then: 'it should fail'
        exception.getMessage().contains('Mappings in Correlated WireMock servers must either have a CorrelationPath header or a Proxy header')
    }

    def 'shouldRejectMappingsWithoutAPriority'() {
        when: 'I register a mapping without a priority'
        def exception = null
        try {
            wireMock.register(get(urlEqualTo("asdf")).withHeader(HeaderName.ofTheCorrelationKey(), WireMock.equalTo("asdf")).willReturn(aResponse().proxiedFrom("123413242")).build());
            fail()
        } catch (BadMappingException | VerificationException e) {
            exception = e
        }
        then: 'it should fail'
        exception.getMessage().contains('Mappings in Correlated WireMock servers must have a priority, ideally calculated from the scope it was created')
    }

    def 'shouldRejectMappingsWithAScenario'() {
        when: 'I register a mapping with a scenario'
        def exception = null
        try {
            wireMock.register(get(urlEqualTo("asdf")).withHeader(HeaderName.ofTheCorrelationKey(), WireMock.equalTo("asdf")).willReturn(aResponse().proxiedFrom("123413242")).atPriority(1).inScenario("some_fake_scenario").build());
            fail();
        } catch (BadMappingException | VerificationException e) {
            exception = e
        }
        then: 'it should fail'
        exception.getMessage().contains('Mappings in Correlated WireMock servers cannot be associated with a WireMock scenario')
    }

}
