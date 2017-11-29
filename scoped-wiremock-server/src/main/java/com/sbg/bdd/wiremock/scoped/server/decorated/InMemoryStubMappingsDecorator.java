package com.sbg.bdd.wiremock.scoped.server.decorated;

import com.github.tomakehurst.wiremock.http.HttpHeader;
import com.github.tomakehurst.wiremock.http.HttpHeaders;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.matching.MultiValuePattern;
import com.github.tomakehurst.wiremock.matching.StringValuePattern;
import com.github.tomakehurst.wiremock.matching.ValueMatcher;
import com.github.tomakehurst.wiremock.stubbing.InMemoryStubMappings;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.github.tomakehurst.wiremock.stubbing.SortedConcurrentMappingSet;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import com.google.common.base.Optional;
import com.sbg.bdd.wiremock.scoped.admin.BadMappingException;
import com.sbg.bdd.wiremock.scoped.admin.model.CorrelationState;
import com.sbg.bdd.wiremock.scoped.integration.HeaderName;
import com.sbg.bdd.wiremock.scoped.integration.RuntimeCorrelationState;
import com.sbg.bdd.wiremock.scoped.integration.URLHelper;
import com.sbg.bdd.wiremock.scoped.server.CorrelatedScopeAdmin;
import com.sbg.bdd.wiremock.scoped.server.SequenceNumberMatcher;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import static com.sbg.bdd.wiremock.scoped.common.Reflection.getValue;
import static com.sbg.bdd.wiremock.scoped.server.ScopePathMatcher.matches;

public class InMemoryStubMappingsDecorator extends InMemoryStubMappings {
    private InMemoryStubMappings delegate;
    private SortedConcurrentMappingSet mappings;
    private CorrelatedScopeAdmin scopeAdmin;

    public InMemoryStubMappingsDecorator(InMemoryStubMappings delegate, CorrelatedScopeAdmin scopeAdmin) {
        this.delegate = delegate;
        this.mappings = getValue(delegate, "mappings");
        this.scopeAdmin = scopeAdmin;
    }

    @Override
    public ServeEvent serveFor(Request request) {
        RequestDecorator requestDecorator = rectifyRequestHeaders(request);
        HttpHeader correlationPath = requestDecorator.getHeaders().getHeader(HeaderName.ofTheCorrelationKey());
        HttpHeader threadContextId = requestDecorator.getHeaders().getHeader(HeaderName.ofTheThreadContextId());
        if (correlationPath.isPresent() && threadContextId.isPresent()) {
            CorrelationState correlatedScope = scopeAdmin.getCorrelatedScope(correlationPath.firstValue());
            correlatedScope.findOrCreateServiceInvocationCount(Integer.valueOf(threadContextId.firstValue()), serviceIdentifierOf(request)).increment();
        }
        //TODO could optimize by grouping StubMappings by correlationPath
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
            StringValuePattern stringValuePattern = extractCorrelationPathPattern(mapping);
            if (stringValuePattern != null && matches(scopePath, stringValuePattern)) {
                iterator.remove();
            }
        }
    }

    private StringValuePattern extractCorrelationPathPattern(StubMapping mapping) {
        StringValuePattern stringValuePattern = null;
        if (mapping.getRequest() != null && mapping.getRequest().getHeaders() != null) {
            MultiValuePattern correlationPattern = mapping.getRequest().getHeaders().get(HeaderName.ofTheCorrelationKey());
            stringValuePattern = correlationPattern == null ? null : correlationPattern.getValuePattern();
        } else if (mapping.getRequest().hasCustomMatcher()) {
            ValueMatcher<Request> customMatcher = getValue(mapping.getRequest(), "matcher");
            if (customMatcher instanceof SequenceNumberMatcher) {
                stringValuePattern = ((SequenceNumberMatcher) customMatcher).getCorrelationPattern();
            }
        }
        return stringValuePattern;
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
        if (!(mapping.getRequest().hasCustomMatcher() || hasCorrelationHeader(mapping))) {
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

    public static String serviceIdentifierOf(Request request) {
        try {
            return URLHelper.identifier(new URL(request.getAbsoluteUrl()), request.getMethod().getName());
        } catch (MalformedURLException e) {
            throw new IllegalStateException(e);
        }
    }
}
