package com.sbg.bdd.wiremock.scoped.admin;

import com.github.tomakehurst.wiremock.admin.AdminTask;
import com.github.tomakehurst.wiremock.admin.model.PathParams;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.common.Json;
import com.github.tomakehurst.wiremock.core.Admin;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import com.github.tomakehurst.wiremock.matching.*;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import com.sbg.bdd.wiremock.scoped.admin.endpointconfig.RemoteEndPointConfigRegistry;
import com.sbg.bdd.wiremock.scoped.admin.model.ExtendedRequestPattern;
import com.sbg.bdd.wiremock.scoped.admin.model.ExtendedStubMapping;
import com.sbg.bdd.wiremock.scoped.integration.EndpointConfig;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_CREATED;

public class CreateStubMappingTask implements AdminTask {
    RemoteEndPointConfigRegistry endPointConfigRegistry;

    @Override
    public ResponseDefinition execute(Admin admin, Request request, PathParams pathParams) {
        ExtendedStubMapping newMapping = Json.read(request.getBodyAsString(), ExtendedStubMapping.class);
        try {
            List<StubMapping> children = createChildrenIfNecessary(newMapping);
            for (StubMapping child : children) {
                admin.addStubMapping(child);
            }
            return ResponseDefinitionBuilder.jsonResponse(newMapping, HTTP_CREATED);
        } catch (BadMappingException e) {
            return ResponseDefinitionBuilder.responseDefinition().
                    withStatus(HTTP_BAD_REQUEST).
                    withBody(e.getMessage()).build();
        } finally {
        }
    }

    public RemoteEndPointConfigRegistry getEndPointConfigRegistry() {
        return endPointConfigRegistry;
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

    private List<StubMapping> createChildrenIfNecessary(ExtendedStubMapping stubMapping) {
        List<StubMapping> result = new ArrayList<>();
        ExtendedRequestPattern requestPatternBuilder = stubMapping.getRequest();
        if (requestPatternBuilder.isToAllKnownExternalServices()) {
            Set<EndpointConfig> allEndpoints = getEndPointConfigRegistry().allKnownExternalEndpoints();
            for (EndpointConfig config : allEndpoints) {
                if (requestPatternBuilder.getEndpointCategory() == null || config.getCategories().contains(requestPatternBuilder.getEndpointCategory())) {
                    RequestPattern requestPattern = buildRequestPattern(urlPathMatching(config.getUrl().getPath() + ".*"), requestPatternBuilder);
                    ResponseDefinition responseDefinition = buildResponseDefinition(stubMapping, config.getPropertyName());
                    StubMapping childStubMapping = buildStubMapping(stubMapping, requestPattern, responseDefinition);
                    result.add(childStubMapping);
                }
            }
        } else {
            RequestPattern requestPattern = buildRequestPattern(calculateUrlPattern(stubMapping), requestPatternBuilder);
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
        return childStubMapping;
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
