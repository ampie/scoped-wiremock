package com.sbg.bdd.wiremock.scoped.recording

import com.sbg.bdd.wiremock.scoped.recording.strategies.CountMatcher

class WhenMatchingCounts extends WhenWorkingWithWireMock {

    def 'should match a range excluding the limits'() throws Exception {

        given:

        when:
        def matcher = CountMatcher.between(5).and(7)

        then:
        matcher.matches(6) == true
        matcher.matches(5) == false
        matcher.matches(7) == false

    }
}
