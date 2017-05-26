package com.sbg.bdd.wiremock.scoped.common;

public interface HasBaseUrl {
    int port();
    //TODO
    //int httpsPort();
    String host();
    String baseUrl();
}
