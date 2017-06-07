package com.sbg.bdd.wiremock.scoped.recording

import com.github.tomakehurst.wiremock.common.Json
import com.sbg.bdd.wiremock.scoped.recording.strategies.ProxyStrategies
import groovy.json.JsonSlurper

import static com.github.tomakehurst.wiremock.http.RequestMethod.PUT
import static com.sbg.bdd.wiremock.scoped.recording.strategies.ProxyStrategies.beIntercepted
import static com.sbg.bdd.wiremock.scoped.recording.strategies.ProxyStrategies.target
import static com.sbg.bdd.wiremock.scoped.recording.strategies.RequestStrategies.*

class WhenBuildingProxyMappings extends WhenWorkingWithWireMock {

    def 'should create a simple proxy mapping'() throws Exception {
        given:
        def wireMockContext = initializeWireMockContext()
        when:
        a(PUT).to("/home/path").to(ProxyStrategies.proxyTo("http://some.host.com/base")).applyTo(wireMockContext)

        then:
        def mappings = wireMockContext.mappings
        mappings.size() == 1
        def mapping = new JsonSlurper().parseText(Json.write(mappings[0]))
        mapping['request']['urlPathPattern'] == '/home/path.*'
        mapping['response']['proxyBaseUrl'] == "http://some.host.com/base"
        mapping['priority'] == DefaultMappingPriority.FALLBACK_PROXY.priority()
    }

    def 'should create an intercepting proxy mapping that targets the original service'() throws Exception {
        given:
        def wireMockContext = initializeWireMockContext()

        when:
        a(PUT).to("some.property.name").to(beIntercepted()).applyTo(wireMockContext)

        then:
        def mappings = wireMockContext.mappings
        mappings.size() == 1
        def mapping = new JsonSlurper().parseText(Json.write(mappings[0]))
        mapping['request']['urlPathPattern'] == '/resolved/endpoint.*'
        mapping['response']['proxyBaseUrl'] == "http://somehost.com"
        mapping['priority'] == DefaultMappingPriority.FALLBACK_PROXY.priority()
    }

    def 'should target the service under test'() throws Exception {
        given:
        def wireMockContext = initializeWireMockContext()
        when:
        a(PUT).to("some.property.name").will(target()
                .theServiceUnderTest()
                .using()
                .theLast(5)
                .segments()).applyTo(wireMockContext)

        then:
        def mappings = wireMockContext.mappings
        mappings.size() == 1
        def mapping = new JsonSlurper().parseText(Json.write(mappings[0]))
        mapping['request']['urlPathPattern'] == '/resolved/endpoint.*'
        mapping['response']['proxyBaseUrl'] == 'http://service.com/under/test'
        mapping['response']['transformers'][0] == 'ProxyUrlTransformer'
        mapping['response']['transformerParameters']['action'] == 'use'
        mapping['response']['transformerParameters']['which'] == 'trailing'
        mapping['response']['transformerParameters']['numberOfSegments'] == 5
        mapping['priority'] == DefaultMappingPriority.SPECIFIC_PROXY.priority()
    }

    def 'should target a specified url'() throws Exception {
        given:
        def wireMockContext = initializeWireMockContext()

        when:
            a(PUT).to("some.property.name").to(
                    target('http://target.com/base')
                            .ignoring()
                            .theFirst(5)
                            .segments()).applyTo(wireMockContext)

        then:
        def mappings = wireMockContext.mappings
        mappings.size() == 1
        def mapping = new JsonSlurper().parseText(Json.write(mappings[0]))
        mapping['request']['urlPathPattern'] == '/resolved/endpoint.*'
        mapping['response']['proxyBaseUrl'] == 'http://target.com/base'
        mapping['response']['transformers'][0] == 'ProxyUrlTransformer'
        mapping['response']['transformerParameters']['action'] == 'ignore'
        mapping['response']['transformerParameters']['which'] == 'leading'
        mapping['response']['transformerParameters']['numberOfSegments'] == 5
        mapping['priority'] == DefaultMappingPriority.SPECIFIC_PROXY.priority()
    }
}
