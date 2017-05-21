package com.github.ampie.wiremock;

import com.github.ampie.wiremock.common.HeaderName;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.matching.ContainsPattern;
import com.github.tomakehurst.wiremock.matching.EqualToPattern;
import com.github.tomakehurst.wiremock.matching.RegexPattern;
import com.github.tomakehurst.wiremock.matching.StringValuePattern;

import java.util.regex.Pattern;

public class ScopePathMatcher {
    public  static   boolean matches(String correlationPath, StringValuePattern valuePattern) {
        String pattern = null;
        if (valuePattern instanceof RegexPattern) {
            pattern = ((RegexPattern) valuePattern).getMatches();
            return pattern.startsWith(correlationPath);
        } else if (valuePattern instanceof ContainsPattern) {
            pattern = ((ContainsPattern) valuePattern).getContains();
            return pattern.startsWith(correlationPath + "/") || pattern.equals(correlationPath);
        } else if (valuePattern instanceof EqualToPattern) {
            pattern = ((EqualToPattern) valuePattern).getEqualTo();
            return pattern.startsWith(correlationPath + "/") || pattern.equals(correlationPath);
        }
        return false;
    }

    public  static boolean matches(Pattern scopePathPattern, Request request) {
        String header = request.getHeader(HeaderName.ofTheCorrelationKey());
        return header != null && scopePathPattern.matcher(header).find();
    }


}
