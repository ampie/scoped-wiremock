package com.sbg.bdd.wiremock.scoped.server

import com.github.tomakehurst.wiremock.matching.ContainsPattern
import com.github.tomakehurst.wiremock.matching.EqualToPattern
import com.github.tomakehurst.wiremock.matching.RegexPattern
import spock.lang.Specification

import static com.sbg.bdd.wiremock.scoped.server.ScopePathMatcher.matches
import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertTrue

class WhenMatchingScopePaths extends Specification {
    def 'shouldMatchThese'() {
        when:''
        then:
        assertTrue(matches("123", new RegexPattern("123.*")));
        assertTrue(matches("123", new RegexPattern("123/456.*")));
        assertTrue(matches("123", new ContainsPattern("123")));
        assertTrue(matches("123", new ContainsPattern("123/456")));
        assertTrue(matches("123", new EqualToPattern("123")));
        assertTrue(matches("123", new EqualToPattern("123/456")));
    }

    def 'shouldNotMatchThese'() {
        when:''
        then:
        assertFalse(matches("456", new RegexPattern("123.*")));
        assertFalse(matches("456", new RegexPattern("123/456.*")));
        assertFalse(matches("456", new ContainsPattern("123")));
        assertFalse(matches("456", new ContainsPattern("123/456")));
        assertFalse(matches("456", new ContainsPattern("4567/89")));
        assertFalse(matches("456", new EqualToPattern("123/456")));
    }
}
