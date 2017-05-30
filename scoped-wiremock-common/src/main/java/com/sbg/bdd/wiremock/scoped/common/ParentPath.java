package com.sbg.bdd.wiremock.scoped.common;

public class ParentPath {
    public static String of(String scopePath) {
        if(scopePath.indexOf("/")>0) {
            return scopePath.substring(0, scopePath.lastIndexOf("/") );
        }else{
            return null;
        }
    }

}
