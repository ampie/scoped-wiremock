package com.github.ampie.wiremock;

public class ExecutionPathExtractor {
    public static String executionScopePathFrom(String scopePath) {
        if(scopePath.indexOf("/")>0) {
            return scopePath.substring(0, scopePath.lastIndexOf("/") );
        }else{
            return scopePath;
        }
    }

}
