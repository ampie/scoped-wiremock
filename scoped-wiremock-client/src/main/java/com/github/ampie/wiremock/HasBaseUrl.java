package com.github.ampie.wiremock;

public interface HasBaseUrl {
    int port();
    //TODO
    //int httpsPort();
    String host();
    String baseUrl();
}
