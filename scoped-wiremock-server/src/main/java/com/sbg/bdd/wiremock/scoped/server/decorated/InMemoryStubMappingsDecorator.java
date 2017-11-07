package com.sbg.bdd.wiremock.scoped.server.decorated;

import com.github.tomakehurst.wiremock.http.HttpHeader;
import com.github.tomakehurst.wiremock.http.HttpHeaders;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.matching.MultiValuePattern;
import com.github.tomakehurst.wiremock.stubbing.*;
import com.google.common.base.Optional;
import com.sbg.bdd.wiremock.scoped.admin.BadMappingException;
import com.sbg.bdd.wiremock.scoped.integration.HeaderName;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import static com.sbg.bdd.wiremock.scoped.common.Reflection.getValue;
import static com.sbg.bdd.wiremock.scoped.server.ScopePathMatcher.matches;

public class InMemoryStubMappingsDecorator extends InMemoryStubMappings {
    private InMemoryStubMappings delegate;
    private SortedConcurrentMappingSet mappings;

    public InMemoryStubMappingsDecorator(InMemoryStubMappings delegate) {
        this.delegate = delegate;
        this.mappings = getValue(delegate,"mappings");
    }

    @Override
    public ServeEvent serveFor(Request request) {
        RequestDecorator requestDecorator = rectifyRequestHeaders(request);
        return delegate.serveFor(requestDecorator);
    }

    public static RequestDecorator rectifyRequestHeaders(Request request) {
        RequestDecorator requestDecorator = new RequestDecorator(request);
        listifyIncorrectlyConcatenatedHeaders(requestDecorator, HeaderName.ofTheEndpointCategory());
        listifyIncorrectlyConcatenatedHeaders(requestDecorator, HeaderName.ofTheServiceInvocationCount());
        return requestDecorator;
    }
    private static void listifyIncorrectlyConcatenatedHeaders(RequestDecorator request, String key) {
        HttpHeaders headers = request.getHeaders();
        HttpHeader header = headers.getHeader(key);
        if (header.isPresent() && header.values().size() == 1) {
            String values = header.firstValue();
            if (values.contains(",")) {
                List<String> valueList = new ArrayList<>();
                for (String value : values.split(",")) {
                    valueList.add(value);
                }
                request.putHeader(key, valueList);
            }
        }
    }

    private boolean hasCorrelationHeader(StubMapping mapping) {
        return mapping.getRequest().getHeaders() != null && (mapping.getRequest().getHeaders().containsKey(HeaderName.ofTheCorrelationKey()) || mapping.getRequest().getHeaders().containsKey(HeaderName.toProxyUnmappedEndpoints()));
    }


    public void removeMappingsForScope(String scopePath) {
        Iterator<StubMapping> iterator = mappings.iterator();
        while (iterator.hasNext()) {
            StubMapping mapping = iterator.next();
            if (mapping.getRequest() != null && mapping.getRequest().getHeaders() != null) {
                MultiValuePattern correlationPattern = mapping.getRequest().getHeaders().get(HeaderName.ofTheCorrelationKey());
                if (correlationPattern != null && matches(scopePath, correlationPattern.getValuePattern())) {
                    iterator.remove();
                }
            }
        }
    }

    public List<StubMapping> findMappingsForScope(String scopePath) {
        Iterator<StubMapping> iterator = mappings.iterator();
        List<StubMapping> result = new ArrayList<>();
        while (iterator.hasNext()) {
            StubMapping mapping = iterator.next();
            if (mapping.getRequest() != null && mapping.getRequest().getHeaders() != null) {
                MultiValuePattern correlationPattern = mapping.getRequest().getHeaders().get(HeaderName.ofTheCorrelationKey());
                if (correlationPattern != null && correlationPattern.getValuePattern().match(scopePath).isExactMatch()) {
                    result.add(mapping);
                }
            }
        }
        return result;
    }

    @Override
    public void addMapping(StubMapping mapping) {
        if (!hasCorrelationHeader(mapping)) {
            throw new BadMappingException("Mappings in Correlated WireMock servers must either have a CorrelationPath header or a Proxy header");
        } else if (mapping.getPriority() == null) {
            throw new BadMappingException("Mappings in Correlated WireMock servers must have a priority, ideally calculated from the scope it was created");
        } else if (mapping.getScenarioName() != null) {
            throw new BadMappingException("Mappings in Correlated WireMock servers cannot be associated with a WireMock scenario");
        } else {
            delegate.addMapping(mapping);
        }
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
