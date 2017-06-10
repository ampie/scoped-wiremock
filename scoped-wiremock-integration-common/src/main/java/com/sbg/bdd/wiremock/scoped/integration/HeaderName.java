package com.sbg.bdd.wiremock.scoped.integration;

public class HeaderName {
    public static String ofTheCorrelationKey() {
        return System.getProperty("scoped.wiremock.correllation.key.header", "x-sbg-messageTraceId");
    }
    public static String ofTheSequenceNumber() {
        return System.getProperty("scoped.wiremock.sequence.number.header", "x-sbg-sequence-number");
    }
    public static String ofTheEndpointCategory() {
        return System.getProperty("scoped.wiremock.endpoiont.category", "x-endpoint-category");
    }
    public static String ofTheResponseCode() {
        return System.getProperty("scoped.wiremock.response.code.header", "HTTP-Response-Code");
    }
    public static String ofTheServiceInvocationCount() {
        return System.getProperty("scoped.wiremock.service.invocation.counts.header", "x.service.invocation.counts");
    }
    public static String toProxyUnmappedEndpoints() {
        return System.getProperty("scoped.wiremock.proxy.unmapped.endpoints.header", "automation.proxy.unmapped.endpoints");
    }
}
