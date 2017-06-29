package com.sbg.bdd.wiremock.scoped.client.strategies;


import com.sbg.bdd.wiremock.scoped.client.WireMockContext;
import com.sbg.bdd.wiremock.scoped.client.builders.ExtendedMappingBuilder;
import com.sbg.bdd.wiremock.scoped.client.builders.ExtendedResponseDefinitionBuilder;
import com.sbg.bdd.wiremock.scoped.client.builders.ResponseStrategy;

import static com.sbg.bdd.wiremock.scoped.client.strategies.ResponseBodyStrategies.aResponse;
import static java.lang.String.format;


public abstract class ProxyStrategies {
    public static ProxyMappingBuilder target(){
        return new ProxyMappingBuilder(null);
    }
    public static ProxyMappingBuilder target(final String baseUrl){
        return new ProxyMappingBuilder(baseUrl);
    }


    public static ResponseStrategy proxyTo(final String baseUrl) {
        return new ResponseStrategy() {
            public ExtendedResponseDefinitionBuilder applyTo(ExtendedMappingBuilder builder, WireMockContext context) throws Exception {
                builder.getRequestPatternBuilder().changeUrlToPattern();
                builder.atPriority(context.calculatePriority(5));
                return aResponse().proxiedFrom(baseUrl);
            }
            @Override
            public String getDescription() {
                return format("proxy to \"%s\"",baseUrl);
            }
        };
    }


    public static ResponseStrategy beIntercepted() {
        return new ResponseStrategy() {
            public ExtendedResponseDefinitionBuilder applyTo(ExtendedMappingBuilder builder, WireMockContext context) throws Exception {
                builder.atPriority(context.calculatePriority(5));
                builder.getRequestPatternBuilder().changeUrlToPattern();
                return aResponse().interceptedFromSource();
            }
            @Override
            public String getDescription() {
                return format("be intercepted");
            }

        };

    }

    /**
     * For use from the ProxyMappingBuiler only
     * @param baseUrl
     * @param segments
     * @param action
     * @param which
     * @return
     */
    static ResponseStrategy target(final String baseUrl, final int segments, final String action, final String which) {
        return new ResponseStrategy() {
            public ExtendedResponseDefinitionBuilder applyTo(ExtendedMappingBuilder builder, WireMockContext context) throws Exception {
                builder.getRequestPatternBuilder().changeUrlToPattern();
                builder.atPriority(context.calculatePriority(4));
                String baseUrlToUse=baseUrl == null?context.getBaseUrlOfServiceUnderTest():baseUrl;
                return aResponse().proxiedFrom(baseUrlToUse).withTransformers("ProxyUrlTransformer")
                        .withTransformerParameter("numberOfSegments", segments)
                        .withTransformerParameter("action", action)
                        .withTransformerParameter("which", which);
            }
            @Override
            public String getDescription() {
                return format("proxy to \"%s\"",baseUrl);
            }

        };
    }


}
