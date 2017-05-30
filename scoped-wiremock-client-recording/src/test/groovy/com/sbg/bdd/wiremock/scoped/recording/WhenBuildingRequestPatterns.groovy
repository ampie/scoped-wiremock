package com.sbg.bdd.wiremock.scoped.recording

import com.github.tomakehurst.wiremock.common.Json
import com.sbg.bdd.wiremock.scoped.recording.strategies.ResponseBodyStrategies
import groovy.json.JsonSlurper

import static com.github.tomakehurst.wiremock.http.RequestMethod.PUT
import static com.sbg.bdd.wiremock.scoped.recording.strategies.RequestStrategies.*
import static com.sbg.bdd.wiremock.scoped.recording.strategies.ResponseBodyStrategies.returnTheBody

class WhenBuildingRequestPatterns extends WhenWorkingWithWireMock {
    def 'should include the scope path of the current user as a header requirement'() throws Exception {

        given:
        def wiremockContext = initializeWireMockContext()

        when:

                a(PUT).to("/home/path").to(returnTheBody("blah", "text/plain")).applyTo(wiremockContext)

        then:
        def mappings = wiremockContext.mappings
        mappings.size() == 1
        def mapping = new JsonSlurper().parseText(Json.write(mappings[0]))
        mapping['request']['url'] == '/home/path'
        mapping['response']['headers']['Content-StepEventType'] == 'text/plain'
        mapping['response']['body'] == 'blah'
    }

    def 'should resolve request paths that appear to be properties from the EndpointConfigRegistry provided'() throws Exception {

        given:
        def wiremockContext = initializeWireMockContext()

        when:
        anyRequest().to("some.property.name").will(returnTheBody("blah", "text/plain")).applyTo(wiremockContext)
        then:
        def mappings = wiremockContext.mappings
        mappings.size() == 1
        def mapping = new JsonSlurper().parseText(Json.write(mappings[0]))
        mapping['request']['url'] == '/resolved/endpoint'
        mapping['response']['headers']['Content-StepEventType'] == 'text/plain'
        mapping['response']['body'] == 'blah'
    }

    def 'should generate multiple mappings when setting up rules that target any known downstream endpoint'() throws Exception {

        given:
        def wiremockContext = initializeWireMockContext()

        when:
        anyRequest().toAnyKnownExternalService().to(returnTheBody("blah", "text/plain")).applyTo(wiremockContext)

        then:
        def mappings = wiremockContext.mappings
        mappings.size() == 2
        println Json.write(mappings)
        def mapping0 = new JsonSlurper().parseText(Json.write(mappings[0]))
        mapping0['request']['urlPattern'] == '/service/one/endpoint.*'
        def mapping1 = new JsonSlurper().parseText(Json.write(mappings[1]))
        mapping1['request']['urlPattern'] == '/service/two/endpoint.*'
        mapping1['response']['headers']['Content-StepEventType'] == 'text/plain'
        mapping1['response']['body'] == 'blah'
    }

    def 'should generate multiple bodypatterns using multiple containing strings'() throws Exception {

        given:
        def wiremockContext = initializeWireMockContext()

        when:
        anyRequest().to("/test/blah").withRequestBody(containing("1", "2", "3")).to(returnTheBody("blah", "text/plain")).applyTo(wiremockContext)

        then:
        def mappings = wiremockContext.mappings
        mappings.size() == 1
        def mapping = new JsonSlurper().parseText(Json.write(mappings[0]))
        println mapping
        mapping['request']['bodyPatterns'][0]['contains'] == '1'
        mapping['request']['bodyPatterns'][1]['contains'] == '2'
        mapping['request']['bodyPatterns'][2]['contains'] == '3'
    }


}
