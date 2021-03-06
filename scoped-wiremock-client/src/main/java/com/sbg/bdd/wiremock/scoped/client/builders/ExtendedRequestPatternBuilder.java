package com.sbg.bdd.wiremock.scoped.client.builders;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.http.RequestMethod;
import com.github.tomakehurst.wiremock.matching.*;
import com.sbg.bdd.wiremock.scoped.admin.model.ExtendedRequestPattern;
import com.sbg.bdd.wiremock.scoped.integration.HeaderName;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static com.sbg.bdd.wiremock.scoped.common.Reflection.getValue;
import static com.sbg.bdd.wiremock.scoped.common.Reflection.setValue;

public class ExtendedRequestPatternBuilder<T extends ExtendedRequestPatternBuilder> extends RequestPatternBuilder {
    private String urlInfo;
    private String pathSuffix;
    private boolean urlIsPattern = false;
    private boolean toAllKnownExternalServices = false;
    private String endpointCategory;
    private String correlationPath;


    public ExtendedRequestPatternBuilder(ExtendedRequestPatternBuilder builder) {
        super();
        copyFrom(builder);
    }

    public ExtendedRequestPatternBuilder(RequestMethod requestMethod) {
        super(requestMethod, null);
    }

    private void copyFrom(ExtendedRequestPatternBuilder builder) {
        this.urlInfo = builder.urlInfo;
        this.pathSuffix = builder.pathSuffix;
        this.urlIsPattern = builder.urlIsPattern;
        this.toAllKnownExternalServices = builder.toAllKnownExternalServices;
        this.endpointCategory=builder.endpointCategory;
        this.correlationPath=builder.correlationPath;
        setValue(this, "method", getValue(builder, "method"));
        setValue(this, "headers", new HashMap<>((Map<? extends String, ? extends MultiValuePattern>) getValue(builder, "headers")));
        setValue(this, "queryParams", new HashMap<>((Map<? extends String, ? extends MultiValuePattern>) getValue(builder, "queryParams")));
        setValue(this, "bodyPatterns", new ArrayList<>((Collection<? extends MultiValuePattern>) getValue(builder, "bodyPatterns")));
        setValue(this, "cookies", new HashMap<>((Map<? extends String, ? extends StringValuePattern>) getValue(builder, "cookies")));
        setValue(this, "basicCredentials", getValue(builder, "basicCredentials"));
    }

    public ExtendedRequestPatternBuilder(ExtendedRequestPatternBuilder requestPatternBuilder, RequestMethod method) {
        super(method, null);
        copyFrom(requestPatternBuilder);
    }


    public T toAnyKnownExternalService() {
        toAllKnownExternalServices = true;
        return (T) this;
    }

    public String getPathSuffix() {
        return pathSuffix;
    }

    public T toAny(String category) {
        return (T) toAnyKnownExternalService().ofCategory(category);
    }

    public T service() {
        return (T) this;
    }

    @Override
    public T withRequestBody(ContentPattern valuePattern) {
        return (T) super.withRequestBody(valuePattern);
    }

    public boolean isToAllKnownExternalServices() {
        return this.toAllKnownExternalServices;
    }

    public void changeUrlToPattern() {
        urlIsPattern = true;
    }

    public T to(String urlInfo, String pathSuffix) {
        this.urlInfo = urlInfo;
        this.pathSuffix = pathSuffix;
        return (T) this;
    }

    public T to(String urlInfo) {
        return to(urlInfo, null);
    }


    public String getUrlInfo() {
        return urlInfo;
    }

    public String getHttpMethod() {
        return ((RequestMethod) getValue(this, "method")).getName();

    }

    public void ensureScopePath(StringValuePattern pattern) {
        Map<String, MultiValuePattern> headers = getValue(this, "headers");
        if (!headers.containsKey(HeaderName.ofTheCorrelationKey())) {
            withHeader(HeaderName.ofTheCorrelationKey(), pattern);
        }
    }

    public ExtendedMappingBuilder will(ResponseStrategy strategy) {
        ExtendedMappingBuilder ruleBuilder = new ExtendedMappingBuilder(this);
        ruleBuilder.will(strategy);
        return ruleBuilder;
    }
//
//    public DownstreamVerification wasMade(final Matcher<Integer> countMatcher) {
//        return new DownstreamVerification() {
//            @Override
//            public void performOnStage(ActorOnStage actorOnStage) {

//            }
//        };
//    }


    public ExtendedMappingBuilder to(ResponseStrategy responseStrategy) {
        return will(responseStrategy);
    }

    public ExtendedMappingBuilder toReturn(ResponseDefinitionBuilder response) {
        ExtendedMappingBuilder ruleBuilder = new ExtendedMappingBuilder(this);
        ruleBuilder.willReturn(response);
        return ruleBuilder;
    }

    public void toAnyKnownExternalService(boolean b) {
        this.toAllKnownExternalServices = b;
    }

    public T withRequestBody(StringValuePattern... bodyPattern) {
        for (StringValuePattern stringValuePattern : bodyPattern) {
            withRequestBody(stringValuePattern);
        }
        return (T) this;
    }

    @Override
    public T withHeader(String key, StringValuePattern valuePattern) {
        return (T) super.withHeader(key, valuePattern);
    }

    public T ofCategory(String p) {
        this.endpointCategory = p;
        withHeader(HeaderName.ofTheEndpointCategory(), WireMock.equalTo(p));
        return (T) this;
    }

    public String getEndpointCategory() {
        return endpointCategory;
    }

    public ExtendedRequestPattern build() {
        ExtendedRequestPattern requestPattern = new ExtendedRequestPattern(correlationPath, super.build());
        requestPattern.setEndpointCategory(endpointCategory);
        requestPattern.setPathSuffix(pathSuffix);
        requestPattern.setToAllKnownExternalServices(toAllKnownExternalServices);
        requestPattern.setUrlInfo(urlInfo);
        requestPattern.setUrlIsPattern(urlIsPattern);
        return requestPattern;
    }

    public void setCorrelationPath(String correlationPath) {
        this.correlationPath = correlationPath;
    }
}
