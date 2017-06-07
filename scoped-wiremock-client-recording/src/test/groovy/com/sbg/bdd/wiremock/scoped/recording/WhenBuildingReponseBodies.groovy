package com.sbg.bdd.wiremock.scoped.recording

import com.github.tomakehurst.wiremock.common.Json
import com.sbg.bdd.wiremock.scoped.recording.strategies.ResponseBodyStrategies
import groovy.json.JsonSlurper

import static com.github.tomakehurst.wiremock.http.RequestMethod.PUT
import static com.sbg.bdd.wiremock.scoped.recording.strategies.RequestStrategies.*
import static com.sbg.bdd.wiremock.scoped.recording.strategies.ResponseBodyStrategies.merge

class WhenBuildingReponseBodies extends WhenWorkingWithWireMock {

    def 'should load the body from a file and headers from the adjacent header file'() throws Exception {
        given:
        def wireMockContext = initializeWireMockContext()

        when:

        a(PUT).to("/home/path").to(ResponseBodyStrategies.returnTheFile("somefile.json")).applyTo(wireMockContext)

        then:
        def mappings = wireMockContext.mappings
        mappings.size() == 1
        def mapping = new JsonSlurper().parseText(Json.write(mappings[0]))
        mapping['request']['urlPath'] == '/home/path'
        mapping['response']['headers']['Content-Type'] == 'application/json'
        mapping['response']['headers']['foo-header'] == 'bar-header-value'
        mapping['response']['body'] == "{\"foo\":\"bar\"}"
        mapping['priority'] == DefaultMappingPriority.BODY_KNOWN.priority()
    }
    def 'should load the body by merging a template with provided variables'() throws Exception{
        given:
            def wiremockContext= initializeWireMockContext()
        when:
        a(PUT).to("/home/path").to(
                        merge(ResponseBodyStrategies.theTemplate("some_template.xml").with("value", "thisValue")
                                .andReturnIt())).applyTo(wiremockContext)

        then:
        def mappings =wiremockContext.mappings
        mappings.size() == 1
        def mapping = new JsonSlurper().parseText(Json.write(mappings[0]))
        mapping['request']['urlPath'] == '/home/path'
        mapping['response']['headers']['Content-Type'] == 'text/xml'
        mapping['response']['headers']['foo-header'] == 'bar-header-value'
        mapping['response']['body'] == "<root>thisValue</root>"
        mapping['priority'] == DefaultMappingPriority.BODY_KNOWN.priority()
    }

}
