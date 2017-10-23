package com.sbg.bdd.wiremock.scoped.server;

import com.github.tomakehurst.wiremock.http.HttpHeader;
import com.github.tomakehurst.wiremock.http.HttpHeaders;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.stubbing.InMemoryStubMappings;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import com.github.tomakehurst.wiremock.stubbing.StubMappings;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import com.google.common.base.Optional;
import com.sbg.bdd.wiremock.scoped.integration.HeaderName;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ScopedStubMappings extends InMemoryStubMappings {
    private InMemoryStubMappings delegate;

    public ScopedStubMappings(InMemoryStubMappings delegate) {
        this.delegate = delegate;
    }

    @Override
    public ServeEvent serveFor(Request request) {
        ScopedRequest scopedRequest = rectifyRequestHeaders(request);
        return delegate.serveFor(scopedRequest);
    }

    public static ScopedRequest rectifyRequestHeaders(Request request) {
        ScopedRequest scopedRequest = new ScopedRequest(request);
        listifyIncorrectlyConcatenatedHeaders(scopedRequest, HeaderName.ofTheEndpointCategory());
        listifyIncorrectlyConcatenatedHeaders(scopedRequest, HeaderName.ofTheServiceInvocationCount());
        return scopedRequest;
    }

    private static void listifyIncorrectlyConcatenatedHeaders(ScopedRequest request, String key) {
        HttpHeaders headers = request.getHeaders();
        HttpHeader header = headers.getHeader(key);
        if (header.isPresent() && header.values().size() == 1) {
            String values = header.firstValue();
            if (values.contains(",")) {
                List<String> valueList=new ArrayList<>();
                for (String value : values.split(",")) {
                    valueList.add(value);
                }
                request.putHeader(key,valueList);
            }
        }
    }

    @Override
    public void addMapping(StubMapping mapping) {
        delegate.addMapping(mapping);
    }

    @Override
    public void removeMapping(StubMapping mapping) {
        delegate.removeMapping(mapping);
    }

    @Override
    public void editMapping(StubMapping stubMapping) {
        delegate.editMapping(stubMapping);
    }

    @Override
    public void reset() {
        delegate.reset();
    }

    @Override
    public void resetScenarios() {
        delegate.resetScenarios();
    }

    @Override
    public List<StubMapping> getAll() {
        return delegate.getAll();
    }

    @Override
    public Optional<StubMapping> get(UUID id) {
        return delegate.get(id);
    }
}
