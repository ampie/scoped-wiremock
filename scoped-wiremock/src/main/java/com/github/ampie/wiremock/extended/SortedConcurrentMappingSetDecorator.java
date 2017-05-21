package com.github.ampie.wiremock.extended;

import com.github.tomakehurst.wiremock.matching.MultiValuePattern;
import com.github.tomakehurst.wiremock.stubbing.SortedConcurrentMappingSet;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import com.github.ampie.wiremock.common.HeaderName;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.function.Consumer;

import static com.github.ampie.wiremock.ScopePathMatcher.matches;

public class SortedConcurrentMappingSetDecorator extends SortedConcurrentMappingSet  {
    SortedConcurrentMappingSet decorated;

    public SortedConcurrentMappingSetDecorator(SortedConcurrentMappingSet decorated) {
        this.decorated = decorated;
    }

    @Override
    public Spliterator<StubMapping> spliterator() {
        return decorated.spliterator();
    }

    @Override
    public void forEach(Consumer<? super StubMapping> action) {
        decorated.forEach(action);
    }

    @Override
    public String toString() {
        return decorated.toString();
    }

    @Override
    public void clear() {
        decorated.clear();
    }

    @Override
    public boolean replace(StubMapping existingStubMapping, StubMapping newStubMapping) {
        return decorated.replace(existingStubMapping, newStubMapping);
    }

    @Override
    public boolean remove(StubMapping mappingToRemove) {
        return decorated.remove(mappingToRemove);
    }

    @Override
    public void add(StubMapping mapping) {
        if (!hasCorrelationHeader(mapping)) {
            throw new BadMappingException("Mappings in Correlated WireMock servers must either have a CorrelationPath header or a Proxy header");
        } else if (mapping.getPriority()==null) {
            throw new BadMappingException("Mappings in Correlated WireMock servers must have a priority, ideally calculated from the scope it was created");
        } else if (mapping.getScenarioName()!=null) {
            throw new BadMappingException("Mappings in Correlated WireMock servers cannot be associated with a WireMock scenario");
        } else {
            decorated.add(mapping);
        }
    }

    private boolean hasCorrelationHeader(StubMapping mapping) {
        return mapping.getRequest().getHeaders() != null && (mapping.getRequest().getHeaders().containsKey(HeaderName.ofTheCorrelationKey()) || mapping.getRequest().getHeaders().containsKey(HeaderName.toProxyUnmappedEndpoints()));
    }


    @Override
    public Iterator<StubMapping> iterator() {
        return decorated.iterator();
    }

    public void removeMappingsForScope(String scopePath) {
        Iterator<StubMapping> iterator = iterator();
        while(iterator.hasNext()){
            StubMapping mapping =iterator.next();
            if (mapping.getRequest() != null && mapping.getRequest().getHeaders() != null) {
                MultiValuePattern correlationPattern = mapping.getRequest().getHeaders().get(HeaderName.ofTheCorrelationKey());
                if (correlationPattern != null && matches(scopePath, correlationPattern.getValuePattern())) {
                    iterator.remove();
                }
            }
        }
    }

    public List<StubMapping> findMappingsForScope(String scopePath) {
        Iterator<StubMapping> iterator = iterator();
        List<StubMapping> result = new ArrayList<>();
        while(iterator.hasNext()){
            StubMapping mapping =iterator.next();
            if (mapping.getRequest() != null && mapping.getRequest().getHeaders() != null) {
                MultiValuePattern correlationPattern = mapping.getRequest().getHeaders().get(HeaderName.ofTheCorrelationKey());
                if (correlationPattern != null && correlationPattern.getValuePattern().match(scopePath).isExactMatch()) {
                    result.add(mapping);
                }
            }
        }
        return result;
    }
}
