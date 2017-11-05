package com.sbg.bdd.wiremock.scoped.common;

public class ParentPath {
    public static String of(String scopePath) {
        if(scopePath.indexOf("/")>0) {
            return scopePath.substring(0, scopePath.lastIndexOf("/") );
        }else{
            return null;//TODO reevaluate this - we now support user scopes
        }
    }
    public static String ofUserScope(String scopePath) {
        if(scopePath.indexOf("/:")>0) {
            return scopePath.substring(0, scopePath.lastIndexOf("/:") );
        }else{
            //It is a user scope
            return scopePath;
        }
    }

}
