package com.github.ampie.wiremock;

import com.github.tomakehurst.wiremock.matching.ContainsPattern;
import com.github.tomakehurst.wiremock.matching.EqualToPattern;
import com.github.tomakehurst.wiremock.matching.RegexPattern;
import org.junit.Test;

import static com.github.ampie.wiremock.ScopePathMatcher.matches;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


public class WhenMatchingScopePaths {
    @Test
    public void shouldMatchThese() {
        assertTrue(matches("123", new RegexPattern("123.*")));
        assertTrue(matches("123", new RegexPattern("123/456.*")));
        assertTrue(matches("123", new ContainsPattern("123")));
        assertTrue(matches("123", new ContainsPattern("123/456")));
        assertTrue(matches("123", new EqualToPattern("123")));
        assertTrue(matches("123", new EqualToPattern("123/456")));
    }

    @Test
    public void shouldNotMatchThese() {
        assertFalse(matches("456", new RegexPattern("123.*")));
        assertFalse(matches("456", new RegexPattern("123/456.*")));
        assertFalse(matches("456", new ContainsPattern("123")));
        assertFalse(matches("456", new ContainsPattern("123/456")));
        assertFalse(matches("123", new ContainsPattern("1234/56")));
        assertFalse(matches("456", new EqualToPattern("123/456")));
    }


}

