package com.sbg.bdd.wiremock.scoped.server;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import com.github.tomakehurst.wiremock.matching.EqualToPattern;
import com.github.tomakehurst.wiremock.matching.RegexPattern;
import com.github.tomakehurst.wiremock.matching.RequestPattern;
import com.github.tomakehurst.wiremock.matching.UrlPathPattern;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import com.sbg.bdd.wiremock.scoped.admin.endpointconfig.RemoteEndPointConfigRegistry;
import com.sbg.bdd.wiremock.scoped.admin.model.ExtendedRequestPattern;
import com.sbg.bdd.wiremock.scoped.admin.model.ExtendedStubMapping;
import com.sbg.bdd.wiremock.scoped.admin.model.ScopeLocalPriority;
import com.sbg.bdd.wiremock.scoped.integration.EndpointConfig;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;

public class ExtendedStubMappingCreator {
    private ExtendedStubMapping stubMapping;
    private CorrelatedScope scope;

    public ExtendedStubMappingCreator(ExtendedStubMapping extendedStubMapping, CorrelatedScope scope) {
        this.stubMapping = extendedStubMapping;
        this.scope = scope;
    }

    private RemoteEndPointConfigRegistry getEndPointConfigRegistry() {
        return scope.getGlobalScope().getEndPointConfigRegistry();
    }

    private RequestPattern buildRequestPattern(UrlPathPattern urlPattern, ExtendedRequestPattern request) {
        return new RequestPattern(urlPattern, request.getMethod(), request.getHeaders(), request.getQueryParameters(), request.getCookies(), request.getBasicAuthCredentials(), request.getBodyPatterns(), request.getCustomMatcher());
    }

    private UrlPathPattern calculateUrlPattern(ExtendedStubMapping newMapping) {
        String path = newMapping.getRequest().getUrlInfo();
        if (isPropertyName(path)) {
            try {
                URL uri = getEndPointConfigRegistry().endpointUrlFor(path).getUrl();
                path = uri.getPath();
            } catch (Exception e) {
                System.out.println(e);
                //TODO Think about this
            }
        }
        if (newMapping.getRequest().getPathSuffix() != null) {
            path = path + newMapping.getRequest().getPathSuffix();
        }
        if (newMapping.getRequest().isUrlIsPattern() && !path.endsWith(".*")) {
            path = path + ".*";
        }
        UrlPathPattern urlPattern;
        if (path.contains(".*")) {
            urlPattern = new UrlPathPattern(new RegexPattern(path), true);
        } else {
            urlPattern = new UrlPathPattern(new EqualToPattern(path), false);
        }
        return urlPattern;
    }


    private boolean isPropertyName(String p) {
        return p.matches("[_a-zA-Z0-9\\.]+");
    }

    public List<StubMapping> createAllSupportingStubMappings() {
        List<StubMapping> result = new ArrayList<>();
        ExtendedRequestPattern sourceRequestPattern = stubMapping.getRequest();
        if (sourceRequestPattern.isToAllKnownExternalServices()) {
            Set<EndpointConfig> allEndpoints = getEndPointConfigRegistry().allKnownExternalEndpoints();
            for (EndpointConfig config : allEndpoints) {
                if (sourceRequestPattern.getEndpointCategory() == null || config.getCategories().contains(sourceRequestPattern.getEndpointCategory())) {
                    RequestPattern requestPattern = buildRequestPattern(urlPathMatching(config.getUrl().getPath() + ".*"), sourceRequestPattern);
                    ResponseDefinition responseDefinition = buildResponseDefinition(stubMapping, config.getPropertyName());
                    StubMapping childStubMapping = buildStubMapping(stubMapping, requestPattern, responseDefinition);
                    result.add(childStubMapping);
                }
            }
        } else {
            RequestPattern requestPattern = buildRequestPattern(calculateUrlPattern(stubMapping), sourceRequestPattern);
            ResponseDefinition responseDefinition = buildResponseDefinition(stubMapping, stubMapping.getRequest().getUrlInfo());
            result.add(buildStubMapping(stubMapping, requestPattern, responseDefinition));
        }
        return result;
    }

    private StubMapping buildStubMapping(ExtendedStubMapping stubMapping, RequestPattern requestPattern, ResponseDefinition responseDefinition) {
        StubMapping childStubMapping = new StubMapping(requestPattern, responseDefinition);
        childStubMapping.setId(UUID.randomUUID());
        childStubMapping.setName(stubMapping.getName());
        childStubMapping.setPersistent(stubMapping.isPersistent());
        childStubMapping.setPriority(calculatePriority(stubMapping.getLocalPriority()));
        return childStubMapping;
    }

    private Integer calculatePriority(ScopeLocalPriority localPriority) {
        final int MAX_LEVELS=10;
        final int PRIORITIES_PER_LEVEL=10;
        return ((MAX_LEVELS - scope.getLevel())*PRIORITIES_PER_LEVEL)+localPriority.priority();
    }

    private ResponseDefinition buildResponseDefinition(ExtendedStubMapping sourceStubMapping, String propertyName) {
        ResponseDefinition result = null;
        if (sourceStubMapping.getResponse() != null) {
            ResponseDefinitionBuilder responseDefinitionBuilder = ResponseDefinitionBuilder.like(sourceStubMapping.getResponse());
            if (Boolean.TRUE.equals(sourceStubMapping.getResponse().getInterceptFromSource())) {
                URL url = getEndPointConfigRegistry().endpointUrlFor(propertyName).getUrl();
                String proxiedBaseUrl = url.getProtocol() + "://" + url.getAuthority();
                responseDefinitionBuilder = responseDefinitionBuilder.proxiedFrom(proxiedBaseUrl);
            }
            result = responseDefinitionBuilder.build();
        }
        return result;
    }
}
