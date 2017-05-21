package com.github.ampie.wiremock.common;

public class HeaderName {
    public static String ofTheCorrelationKey() {
        return System.getProperty("scoped.wiremock.correllation.key.header", "x-sbg-messageTraceId");
    }
    public static String ofTheSequenceNumber() {
        return System.getProperty("scoped.wiremock.sequence.number.header", "x-sbg-sequence-number");
    }
    public static String ofTheResponseCode() {
        return System.getProperty("scoped.wiremock.response.code.header", "HTTP-Response-Code");
    }
    public static String toProxyUnmappedEndpoints() {
        return System.getProperty("scoped.wiremock.proxy.unmapped.endpoints.header", "automation.proxy.unmapped.endpoints");
    }
}
