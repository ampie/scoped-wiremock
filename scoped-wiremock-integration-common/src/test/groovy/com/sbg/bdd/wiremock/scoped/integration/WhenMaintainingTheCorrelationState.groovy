package com.sbg.bdd.wiremock.scoped.integration

import spock.lang.Specification

class WhenMaintainingTheCorrelationState extends Specification {
    def 'it should extract the WireMock host and port from the correlation path'() {
        given:
        def state = new BaseWireMockCorrelationState()

        when:
        state.set('some.host/9090/scope1/scope1_1',true)
        then:
        state.correlationPath == 'some.host/9090/scope1/scope1_1'
        state.wireMockBaseUrl == new URL('http://some.host:9090')
    }
    def 'it should keep track of the number of times a certain service was called'() {
        given:
        def state = new BaseWireMockCorrelationState()
        state.set('some.host/9090/scope1/scope1_1',true)
        state.initSequenceNumberFor('my.endpoint',6)
        when:
        def count= state.getNextSequenceNumberFor('my.endpoint')
        then:
        count == 7
    }
}
